import { useContext, useEffect, useRef, useState } from 'react';
import * as DocumentPicker from 'expo-document-picker';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';
import { useDispatch } from '../store';
import { createComment, ConfirmedCommentMutation } from '../slices/comment';
import type { IFile } from '../models/file';
import { useOffline } from './OfflineProvider';
import { OfflineError, PreparedWorkOrderView } from './types';
import { sessionGeneration, assertSessionGeneration } from './sessionBridge';

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
  const { uploadFiles } = useContext(CompanySettingsContext);
  const dispatch = useDispatch();
  const [submittingComment, setSubmitting] = useState(false);
  const [commentContent, setContent] = useState('');
  const [commentFiles, setFiles] = useState<IFile[]>([]);
  const [commentRefreshPending, setRefreshPending] = useState(false);
  const textRevision = useRef(0);
  const filesRevision = useRef(0);
  const busy = useRef(false);
  const owner = useRef('');
  const generation = sessionGeneration();
  const scope = `${generation}:${id}`;
  const presentation = useRef({ scope, isPrepared, revision: 0 });
  if (presentation.current.scope !== scope)
    presentation.current = { scope, isPrepared, revision: 0 };
  // A download completing while the fiche is open must preserve typed text
  // and picked files. Leaving prepared mode still invalidates old callbacks.
  if (presentation.current.isPrepared && !isPrepared)
    presentation.current.revision += 1;
  presentation.current.isPrepared = isPrepared;
  const key = `${scope}:${presentation.current.revision}`;
  owner.current = key;
  const current = () =>
    owner.current === key && generation === sessionGeneration();
  useEffect(() => {
    owner.current = key;
    busy.current = false;
    textRevision.current = 0;
    filesRevision.current = 0;
    setContent('');
    setFiles([]);
    setSubmitting(false);
    setRefreshPending(false);
    return () => {
      owner.current = '';
    };
  }, [key]);
  // Explicit review of a refused publication can restore its text and private files.
  const review = prepared?.commentDraft;
  useEffect(() => {
    if (review && !textRevision.current) setContent(review.content);
  }, [key, review?.revision]);
  const changeCommentContent = (text: string) => {
    textRevision.current++;
    setContent(text);
  };
  const importCommentFiles = (files: IFile[]) => {
    if (files.length) {
      filesRevision.current++;
      setFiles((old) => [...old, ...files]);
    }
  };
  const removeCommentFile = (index: number) => {
    filesRevision.current++;
    setFiles((old) => old.filter((_, i) => i !== index));
  };
  const pickCommentFile = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        multiple: true,
        copyToCacheDirectory: true
      });
      if (current() && !result.canceled)
        importCommentFiles(
          result.assets.map((asset) => ({
            uri: asset.uri,
            name: asset.name,
            type: asset.mimeType || 'application/octet-stream'
          }))
        );
    } catch (error) {
      if (current()) offline.reportError(error);
    }
  };
  const handleCommentSubmit = async () => {
    const content = commentContent.trim();
    if (busy.current || !canComposeComment || !content) return;
    busy.current = true;
    setSubmitting(true);
    const textAtSubmit = textRevision.current;
    const filesAtSubmit = filesRevision.current;
    const files = commentFiles;
    try {
      if (isPrepared) {
        await offline.publishComment(
          id,
          content,
          files,
          review?.revision ?? null
        );
      } else {
        if (!connectedAvailable) throw new OfflineError('NETWORK_UNAVAILABLE');
        const uploaded = files.length
          ? await uploadFiles(files, [], false)
          : [];
        assertSessionGeneration(generation);
        const result: ConfirmedCommentMutation = await dispatch(
          createComment({
            workOrder: { id },
            content,
            files: uploaded.map((file) => ({ id: file.id }))
          })
        );
        if (current() && result.localUpdateError) {
          setRefreshPending(true);
          offline.reportError(result.localUpdateError);
        }
      }
      if (!current()) return;
      if (textRevision.current === textAtSubmit) {
        textRevision.current = 0;
        setContent('');
      }
      if (filesRevision.current === filesAtSubmit) setFiles([]);
      else setFiles((list) => list.filter((file) => !files.includes(file)));
    } catch (error) {
      if (current()) offline.reportError(error);
    } finally {
      if (current()) {
        busy.current = false;
        setSubmitting(false);
      }
    }
  };
  return {
    submittingComment,
    commentContent,
    commentFiles,
    commentRefreshPending,
    changeCommentContent,
    handleCommentSubmit,
    importCommentFiles,
    pickCommentFile,
    removeCommentFile,
    removeLocalCommentFile: (fileId: string) =>
      review &&
      offline
        .removeDraftFile(id, { kind: 'COMMENT' }, fileId, review.revision)
        .catch(offline.reportError),
    confirmCommentRefresh: (at: number) => {
      if (current() && generation === at) setRefreshPending(false);
    }
  };
}
