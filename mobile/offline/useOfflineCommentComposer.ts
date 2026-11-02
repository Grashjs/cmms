import { useContext, useEffect, useRef, useState } from 'react';
import { Alert } from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import { useTranslation } from 'react-i18next';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';
import { useDispatch } from '../store';
import { createComment, ConfirmedCommentMutation } from '../slices/comment';
import { useOffline } from './OfflineProvider';
import { OfflineError, PreparedWorkOrderView, LocalFile } from './types';
import { sessionGeneration, assertSessionGeneration } from './sessionBridge';

/** Owns only the composer: durable drafts/imports and revision-aware acknowledgement.
 * Connected publication keeps its existing request and confirmation handling. */
export function useOfflineCommentComposer({
  id,
  isPrepared,
  prepared,
  connectedAvailable,
  canComposeComment
}: {
  id: number;
  isPrepared: boolean;
  prepared: PreparedWorkOrderView | null;
  connectedAvailable: boolean;
  canComposeComment: boolean;
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const { uploadFiles } = useContext(CompanySettingsContext);
  const [preparing, setPreparing] = useState(false);
  const [commentContent, setCommentContent] = useState('');
  const commentContentRef = useRef('');
  const [attachmentPending, setAttachmentPending] = useState(false);
  const attachmentPendingRef = useRef(false);
  const [confirmedDraftToClear, setConfirmedDraftToClear] = useState<
    number | null
  >(null);
  const confirmedDraftToClearRef = useRef<number | null>(null);
  const [commentRefreshPending, setCommentRefreshPending] = useState(false);
  const commentAwaitingRefreshId = useRef<number | null>(null);
  const commentSequence = useRef(0);
  const commentWriteSequence = useRef(0);
  const commentSavedRevision = useRef<number | null>(null);
  const commentSaving = useRef(false);
  const commentSubmitting = useRef(false);
  const transferringComment = useRef(false);
  const [commentTransferConflict, setCommentTransferConflict] = useState(false);
  const [savingCommentDraft, setSavingCommentDraft] = useState(false);
  const [commentDraftError, setCommentDraftError] = useState(false);
  const [submittingComment, setSubmittingComment] = useState(false);
  const [commentFiles, setCommentFiles] = useState<
    { uri: string; name: string; type: string }[]
  >([]);
  const commentFilesRevision = useRef(0);
  const [importingFiles, setImportingFiles] = useState(false);
  const importingFilesRef = useRef(false);
  const [localFiles, setLocalFiles] = useState<LocalFile[]>([]);
  const localFileIds = prepared?.commentDraft?.fileIds ?? [];
  const localFilesKey = JSON.stringify(localFileIds);
  const fileImportError = localFiles.some((file) => file.status === 'ERROR');
  const filesReady =
    localFileIds.length === localFiles.length &&
    localFiles.every((file) => file.status === 'READY');
  useEffect(() => {
    setLocalFiles([]);
    if (!offline.repository || !isPrepared || !offline.readable) return;
    const subscription = offline.repository
      .observeFiles(localFileIds)
      .subscribe({ next: setLocalFiles, error: () => setLocalFiles([]) });
    return () => subscription.unsubscribe();
  }, [offline.repository, isPrepared, offline.readable, localFilesKey]);
  const currentSessionGeneration = sessionGeneration();
  const composerContext = useRef({ id, generation: currentSessionGeneration });
  useEffect(() => {
    if (
      composerContext.current.id === id &&
      composerContext.current.generation === currentSessionGeneration
    )
      return;
    composerContext.current = { id, generation: currentSessionGeneration };
    commentSequence.current++;
    commentWriteSequence.current++;
    commentFilesRevision.current++;
    commentSavedRevision.current = null;
    commentSaving.current = false;
    commentSubmitting.current = false;
    transferringComment.current = false;
    setCommentTransferConflict(false);
    setCommentContent('');
    commentContentRef.current = '';
    setAttachmentPending(false);
    attachmentPendingRef.current = false;
    confirmedDraftToClearRef.current = null;
    setConfirmedDraftToClear(null);
    setCommentRefreshPending(false);
    commentAwaitingRefreshId.current = null;
    setCommentFiles([]);
    setImportingFiles(false);
    importingFilesRef.current = false;
    setLocalFiles([]);
    setSavingCommentDraft(false);
    setCommentDraftError(false);
    setSubmittingComment(false);
    setPreparing(false);
  }, [id, currentSessionGeneration]);

  useEffect(() => {
    if (
      !isPrepared ||
      !prepared?.available ||
      transferringComment.current ||
      commentSaving.current ||
      importingFilesRef.current ||
      commentSubmitting.current ||
      commentDraftError
    )
      return;
    // Re-read the durable draft after a deferred import/submit. An older projection
    // must not replace text typed while that write was in progress.
    let active = true;
    const generation = sessionGeneration();
    const sequence = commentSequence.current;
    void offline.repository
      .getCommentDraft(id)
      .then((draft) => {
        if (
          !active ||
          generation !== sessionGeneration() ||
          composerContext.current.id !== id ||
          sequence !== commentSequence.current ||
          commentSaving.current ||
          importingFilesRef.current ||
          commentSubmitting.current ||
          transferringComment.current
        )
          return;
        // Failed cleanup must not turn an already published revision into a new submission.
        if (draft && draft.revision === confirmedDraftToClearRef.current)
          return;
        setCommentContent(draft?.content ?? '');
        commentContentRef.current = draft?.content ?? '';
        setAttachmentPending(!!draft?.attachmentPending);
        attachmentPendingRef.current = !!draft?.attachmentPending;
        commentSavedRevision.current = draft?.revision ?? null;
      })
      .catch((error) => {
        if (active && generation === sessionGeneration())
          offline.reportError(error);
      });
    return () => {
      active = false;
    };
  }, [
    isPrepared,
    prepared?.available,
    prepared?.commentDraft?.revision,
    offline.repository,
    commentDraftError,
    importingFiles,
    submittingComment
  ]);

  useEffect(() => {
    if (
      commentRefreshPending &&
      prepared?.comments.some(
        (comment) => comment.id === commentAwaitingRefreshId.current
      )
    ) {
      commentAwaitingRefreshId.current = null;
      setCommentRefreshPending(false);
    }
  }, [prepared?.comments, commentRefreshPending]);

  const persistCommentComposer = (
    content: string,
    filesPending = attachmentPendingRef.current
  ) => {
    if (transferringComment.current) return;
    setCommentContent(content);
    commentContentRef.current = content;
    setAttachmentPending(filesPending);
    attachmentPendingRef.current = filesPending;
    const sequence = ++commentSequence.current;
    const writeSequence = ++commentWriteSequence.current;
    setCommentDraftError(false);
    if (!isPrepared) return;
    const generation = sessionGeneration();
    commentSaving.current = true;
    setSavingCommentDraft(true);
    setCommentDraftError(false);
    void offline
      .saveCommentDraft(id, content, undefined, filesPending ? true : undefined)
      .then((draft) => {
        if (
          generation !== sessionGeneration() ||
          composerContext.current.id !== id ||
          writeSequence !== commentWriteSequence.current
        )
          return;
        if (sequence === commentSequence.current)
          commentSavedRevision.current = draft.revision;
        commentSaving.current = false;
        setSavingCommentDraft(false);
      })
      .catch(() => {
        if (
          generation !== sessionGeneration() ||
          composerContext.current.id !== id ||
          writeSequence !== commentWriteSequence.current
        )
          return;
        commentSaving.current = false;
        setSavingCommentDraft(false);
        setCommentDraftError(true);
      });
  };
  const changeCommentContent = (content: string) =>
    persistCommentComposer(content);

  const prepareWorkOrder = async () => {
    if (preparing || commentSubmitting.current) return;
    const generation = sessionGeneration();
    const content = commentContent;
    let copied = false;
    transferringComment.current = true;
    setPreparing(true);
    try {
      await offline.prepare(id);
      assertSessionGeneration(generation);
      copied = true;
      if (content.length || attachmentPendingRef.current) {
        const draft = await offline.preserveConnectedCommentDraft(
          id,
          content,
          attachmentPendingRef.current
        );
        assertSessionGeneration(generation);
        if (composerContext.current.id !== id) return;
        commentSavedRevision.current = draft.revision;
        if (commentFiles.length) {
          const selected = commentFiles.slice();
          setCommentFiles([]);
          await offline.importFiles(
            id,
            { kind: 'COMMENT' },
            selected,
            35 * 1024 * 1024
          );
          assertSessionGeneration(generation);
          if (composerContext.current.id !== id) return;
          const imported = await offline.repository.getCommentDraft(id);
          assertSessionGeneration(generation);
          if (composerContext.current.id !== id) return;
          commentSavedRevision.current = imported.revision;
          attachmentPendingRef.current = !!imported.attachmentPending;
          setAttachmentPending(!!imported.attachmentPending);
          setCommentFiles([]);
        }
      } else {
        const draft = await offline.repository.getCommentDraft(id);
        assertSessionGeneration(generation);
        if (composerContext.current.id !== id) return;
        setCommentContent(draft?.content ?? '');
        commentContentRef.current = draft?.content ?? '';
        setAttachmentPending(!!draft?.attachmentPending);
        attachmentPendingRef.current = !!draft?.attachmentPending;
        commentSavedRevision.current = draft?.revision ?? null;
      }
      setCommentDraftError(false);
      setCommentTransferConflict(false);
      transferringComment.current = false;
    } catch (failure) {
      if (
        generation !== sessionGeneration() ||
        composerContext.current.id !== id
      )
        return;
      if (copied && (content.length || attachmentPendingRef.current)) {
        // Keep the connected text visible until the user explicitly resolves the other local draft.
        setCommentDraftError(true);
        setCommentTransferConflict(true);
      } else transferringComment.current = false;
      offline.reportError(failure);
    } finally {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      )
        setPreparing(false);
    }
  };

  const resolveTransferredComment = async (keepDisplayed: boolean) => {
    const generation = sessionGeneration();
    try {
      const existing = await offline.repository.getCommentDraft(id);
      assertSessionGeneration(generation);
      if (composerContext.current.id !== id) return;
      if (!keepDisplayed && !existing) throw new OfflineError('DRAFT_CHANGED');
      const content = keepDisplayed ? commentContent : existing.content;
      const expectedRevision = existing?.revision ?? null;
      const filesPending = keepDisplayed
        ? attachmentPendingRef.current
        : !!existing?.attachmentPending;
      Alert.alert(
        t(keepDisplayed ? 'offline_keep_draft' : 'offline_draft_saved'),
        t(
          keepDisplayed ? 'offline_changed_revision' : 'offline_discard_confirm'
        ),
        [
          { text: t('cancel'), style: 'cancel' },
          {
            text: t(keepDisplayed ? 'offline_keep_draft' : 'offline_discard'),
            style: 'destructive',
            onPress: async () => {
              try {
                assertSessionGeneration(generation);
                if (composerContext.current.id !== id) return;
                const saved = await offline.saveCommentDraft(
                  id,
                  content,
                  expectedRevision,
                  filesPending
                );
                assertSessionGeneration(generation);
                if (composerContext.current.id !== id) return;
                commentSavedRevision.current = saved.revision;
                commentSequence.current++;
                setCommentContent(saved.content);
                commentContentRef.current = saved.content;
                setAttachmentPending(!!saved.attachmentPending);
                attachmentPendingRef.current = !!saved.attachmentPending;
                if (!keepDisplayed) {
                  commentFilesRevision.current++;
                  setCommentFiles([]);
                } else if (commentFiles.length) {
                  await importCommentFiles(commentFiles);
                  assertSessionGeneration(generation);
                  if (composerContext.current.id !== id) return;
                }
                transferringComment.current = false;
                setCommentTransferConflict(false);
                setCommentDraftError(false);
              } catch (failure) {
                if (generation === sessionGeneration())
                  offline.reportError(failure);
              }
            }
          }
        ]
      );
    } catch (failure) {
      if (generation === sessionGeneration()) offline.reportError(failure);
    }
  };
  const clearPublishedCommentDraft = async (revision: number) => {
    try {
      await offline.abandonCommentDraft(id, revision);
    } catch (failure) {
      // A newer draft is not part of this confirmed publication.
      if (
        !(failure instanceof OfflineError) ||
        failure.code !== 'DRAFT_CHANGED'
      )
        throw failure;
    }
  };

  const retryPublishedDraftCleanup = async () => {
    const revision = confirmedDraftToClearRef.current;
    if (revision === null) return;
    const generation = sessionGeneration();
    try {
      await clearPublishedCommentDraft(revision);
      assertSessionGeneration(generation);
      if (composerContext.current.id !== id) return;
      confirmedDraftToClearRef.current = null;
      setConfirmedDraftToClear(null);
    } catch (failure) {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      )
        offline.reportError(failure);
    }
  };

  const handleCommentSubmit = async (textOnlyApproval?: {
    sequence: number;
    revision: number;
  }) => {
    if (
      !commentContentRef.current.trim() ||
      commentSubmitting.current ||
      transferringComment.current ||
      confirmedDraftToClearRef.current !== null ||
      !canComposeComment ||
      importingFilesRef.current ||
      (isPrepared && !filesReady) ||
      (isPrepared && (commentSaving.current || commentDraftError))
    )
      return;
    const sequence = commentSequence.current;
    let revision =
      commentSavedRevision.current ?? prepared?.commentDraft?.revision;
    const content = commentContentRef.current.trim();
    const files = commentFiles.slice();
    const filesRevision = commentFilesRevision.current;
    const generation = sessionGeneration();
    if (
      attachmentPendingRef.current &&
      !files.length &&
      !prepared?.commentDraft?.fileIds?.length &&
      !textOnlyApproval
    ) {
      Alert.alert(
        t('offline_comment_text_only'),
        t('offline_comment_attachment_pending'),
        [
          { text: t('cancel'), style: 'cancel' },
          {
            text: t('offline_comment_text_only'),
            onPress: () => {
              if (
                generation !== sessionGeneration() ||
                composerContext.current.id !== id
              )
                return;
              if (revision !== undefined && revision !== null)
                void handleCommentSubmit({ sequence, revision });
            }
          }
        ]
      );
      return;
    }
    if (
      textOnlyApproval &&
      (textOnlyApproval.sequence !== sequence ||
        textOnlyApproval.revision !== revision)
    ) {
      offline.reportError(new OfflineError('DRAFT_CHANGED'));
      return;
    }
    commentSubmitting.current = true;
    setSubmittingComment(true);
    let publicationConfirmed = false;
    try {
      if (textOnlyApproval) {
        // Consent consumes the exact reviewed draft; typing never clears this marker.
        const saved = await offline.saveCommentDraft(
          id,
          commentContentRef.current,
          revision,
          false
        );
        assertSessionGeneration(generation);
        revision = saved.revision;
        if (composerContext.current.id !== id) return;
        if (commentSequence.current === sequence) {
          commentSavedRevision.current = revision;
          attachmentPendingRef.current = false;
          setAttachmentPending(false);
        }
      }
      if (isPrepared) {
        if (revision === undefined || revision === null)
          throw new OfflineError('DRAFT_CHANGED');
        await offline.submitCommentDraft(id, revision);
        assertSessionGeneration(generation);
        publicationConfirmed = true;
      } else {
        if (!connectedAvailable) throw new OfflineError('NETWORK_UNAVAILABLE');
        let fileIds: { id: number }[] = [];
        if (files.length > 0) {
          const uploadedFiles = await uploadFiles(files, [], false);
          assertSessionGeneration(generation);
          fileIds = uploadedFiles.map((f) => ({ id: f.id }));
        }
        const result: ConfirmedCommentMutation = await dispatch(
          createComment({ workOrder: { id }, content, files: fileIds })
        );
        assertSessionGeneration(generation);
        publicationConfirmed = true;
        if (composerContext.current.id !== id) return;
        if (result.localUpdateError) {
          commentAwaitingRefreshId.current = result.comment?.id ?? null;
          setCommentRefreshPending(true);
          offline.reportError(result.localUpdateError);
        }
        if (isPrepared && revision !== undefined && revision !== null) {
          try {
            await clearPublishedCommentDraft(revision);
          } catch (failure) {
            assertSessionGeneration(generation);
            if (composerContext.current.id !== id) return;
            confirmedDraftToClearRef.current = revision;
            setConfirmedDraftToClear(revision);
            offline.reportError(failure);
          }
        }
      }
    } catch (error) {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      )
        offline.reportError(error);
    } finally {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      ) {
        if (publicationConfirmed) {
          if (commentSequence.current === sequence) {
            commentContentRef.current = '';
            setCommentContent('');
            commentSavedRevision.current = null;
            attachmentPendingRef.current = false;
            setAttachmentPending(false);
          }
          if (commentFilesRevision.current === filesRevision)
            setCommentFiles([]);
        }
        commentSubmitting.current = false;
        setSubmittingComment(false);
      }
    }
  };

  const pickCommentFile = async () => {
    if (
      (!isPrepared && !connectedAvailable) ||
      importingFilesRef.current ||
      !canComposeComment
    )
      return;
    const generation = sessionGeneration();
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        multiple: true,
        copyToCacheDirectory: true
      });
      assertSessionGeneration(generation);
      if (composerContext.current.id !== id) return;
      if (!result.canceled && result.assets) {
        const newFiles = result.assets.map((asset) => ({
          uri: asset.uri,
          name: asset.name,
          type: asset.mimeType || 'application/octet-stream'
        }));
        if (isPrepared) {
          await importCommentFiles(newFiles);
        } else {
          commentFilesRevision.current++;
          setCommentFiles((current) => [...current, ...newFiles]);
          persistCommentComposer(commentContentRef.current, true);
        }
      }
    } catch (error) {
      if (generation === sessionGeneration()) offline.reportError(error);
    }
  };

  const removeCommentFile = (index: number) => {
    commentFilesRevision.current++;
    setCommentFiles((current) => current.filter((_, i) => i !== index));
    if (isPrepared) return;
    // Removing a selection does not silently consent to publishing text alone.
    persistCommentComposer(
      commentContentRef.current,
      isPrepared ? attachmentPendingRef.current : commentFiles.length > 1
    );
  };

  const importCommentFiles = async (files = commentFiles) => {
    if (importingFilesRef.current || !files.length) return;
    const generation = sessionGeneration();
    const sequence = ++commentSequence.current;
    commentFilesRevision.current++;
    importingFilesRef.current = true;
    setImportingFiles(true);
    try {
      await offline.importFiles(
        id,
        { kind: 'COMMENT' },
        files,
        35 * 1024 * 1024
      );
      assertSessionGeneration(generation);
      const draft = await offline.repository.getCommentDraft(id);
      assertSessionGeneration(generation);
      if (composerContext.current.id !== id) return;
      if (sequence === commentSequence.current) {
        commentSavedRevision.current = draft.revision;
        attachmentPendingRef.current = !!draft.attachmentPending;
        setAttachmentPending(!!draft.attachmentPending);
      }
      setCommentFiles([]);
    } catch (error) {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      ) {
        offline.reportError(error);
      }
    } finally {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      ) {
        importingFilesRef.current = false;
        setImportingFiles(false);
      }
    }
  };

  const removeLocalCommentFile = async (fileId: string) => {
    if (importingFilesRef.current || !prepared?.commentDraft) return;
    const generation = sessionGeneration();
    const sequence = ++commentSequence.current;
    importingFilesRef.current = true;
    setImportingFiles(true);
    try {
      await offline.removeDraftFile(
        id,
        { kind: 'COMMENT' },
        fileId,
        prepared.commentDraft.revision
      );
      assertSessionGeneration(generation);
      const draft = await offline.repository.getCommentDraft(id);
      assertSessionGeneration(generation);
      if (
        composerContext.current.id === id &&
        sequence === commentSequence.current
      )
        commentSavedRevision.current = draft?.revision ?? null;
    } finally {
      if (
        generation === sessionGeneration() &&
        composerContext.current.id === id
      ) {
        importingFilesRef.current = false;
        setImportingFiles(false);
      }
    }
  };

  const confirmCommentRefresh = (generation: number) => {
    if (generation === sessionGeneration() && composerContext.current.id === id)
      setCommentRefreshPending(false);
  };
  return {
    preparing,
    commentContent,
    attachmentPending,
    confirmedDraftToClear,
    commentRefreshPending,
    commentTransferConflict,
    savingCommentDraft:
      savingCommentDraft ||
      importingFiles ||
      localFiles.some((file) => file.status === 'IMPORTING'),
    commentDraftError: commentDraftError || fileImportError,
    filesReady,
    submittingComment,
    commentFiles,
    changeCommentContent,
    prepareWorkOrder,
    resolveTransferredComment,
    retryPublishedDraftCleanup,
    handleCommentSubmit,
    pickCommentFile,
    removeCommentFile,
    confirmCommentRefresh,
    removeLocalCommentFile,
    importingFiles,
    importCommentFiles
  };
}
