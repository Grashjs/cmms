import { useEffect, useRef, useState } from 'react';
import { Alert, View } from 'react-native';
import { Button, Text } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import FileUpload from '../../components/FileUpload';
import type File from '../../models/file';
import { useOffline } from '../OfflineProvider';
import type { AttachmentDraft, FileTarget, LocalFile } from '../types';
import { OfflineFileItem } from './OfflineFileItem';
import { assertSessionGeneration, sessionGeneration } from '../sessionBridge';

/** Only this explicit button submits the durable selection, independently of form fields. */
export function OfflineAttachmentEditor({
  workOrderId,
  taskId,
  image = false,
  files = [],
  title,
  showSubmit = true,
  editable = true,
  onRemoveServer
}: {
  workOrderId: number;
  taskId?: number;
  image?: boolean;
  files?: File[];
  title: string;
  showSubmit?: boolean;
  editable?: boolean;
  onRemoveServer?: (fileId: number) => Promise<{ localUpdateError?: unknown }>;
}) {
  const offline = useOffline();
  const { t } = useTranslation();
  const [draft, setDraft] = useState<AttachmentDraft | null>(null);
  const [saving, setSaving] = useState(false);
  const [failed, setFailed] = useState(false);
  const [localFiles, setLocalFiles] = useState<LocalFile[]>([]);
  const [removingServer, setRemovingServer] = useState(false);
  const [confirmedRemoval, setConfirmedRemoval] = useState<number | null>(null);
  const removingServerRef = useRef(false);
  const editorContext = useRef({
    repository: offline.repository,
    workOrderId,
    taskId,
    image
  });
  editorContext.current = {
    repository: offline.repository,
    workOrderId,
    taskId,
    image
  };
  const busy = useRef(false);
  const target: FileTarget = taskId
    ? { kind: 'TASK', taskId }
    : { kind: 'WORK_ORDER', image };
  useEffect(() => {
    setDraft(null);
    busy.current = false;
    setSaving(false);
    setFailed(false);
    setRemovingServer(false);
    removingServerRef.current = false;
    setConfirmedRemoval(null);
    if (!offline.repository || !offline.readable) return;
    const subscription = offline.repository
      .observeAttachmentDraft(workOrderId, taskId)
      .subscribe({
        next: setDraft,
        error: () => setDraft(null)
      });
    return () => subscription.unsubscribe();
  }, [offline.repository, offline.readable, workOrderId, taskId]);
  useEffect(() => {
    // A complete canonical update confirming absence also clears the warning.
    // Renewed signed URLs or unrelated draft edits are not confirmation.
    if (
      confirmedRemoval !== null &&
      !files.some((file) => file.id === confirmedRemoval)
    )
      setConfirmedRemoval(null);
  }, [files, confirmedRemoval]);
  const allIds = [
    ...(draft?.fileIds ?? []),
    ...(draft?.imageFileId ? [draft.imageFileId] : [])
  ];
  const idsKey = JSON.stringify(allIds);
  useEffect(() => {
    setLocalFiles([]);
    if (!offline.repository || !offline.readable) return;
    const subscription = offline.repository
      .observeFiles(allIds)
      .subscribe({ next: setLocalFiles, error: () => setLocalFiles([]) });
    return () => subscription.unsubscribe();
  }, [offline.repository, offline.readable, idsKey]);
  const filesReady =
    allIds.length === localFiles.length &&
    localFiles.every((file) => file.status === 'READY');
  const importing = localFiles.some((file) => file.status === 'IMPORTING');
  const fileError = localFiles.some((file) => file.status === 'ERROR');
  useEffect(() => {
    if (filesReady) setFailed(false);
  }, [filesReady]);
  const run = async (action: () => Promise<unknown>) => {
    if (busy.current) return;
    const generation = sessionGeneration();
    busy.current = true;
    setSaving(true);
    setFailed(false);
    try {
      await action();
      assertSessionGeneration(generation);
    } catch (error) {
      if (generation === sessionGeneration()) {
        setFailed(true);
        offline.reportError(error);
      }
    } finally {
      if (generation === sessionGeneration()) {
        busy.current = false;
        setSaving(false);
      }
    }
  };
  const ids = image
    ? draft?.imageFileId
      ? [draft.imageFileId]
      : []
    : draft?.fileIds ?? [];
  const removeFromServer = (file: File) => {
    const generation = sessionGeneration();
    const context = editorContext.current;
    const isCurrent = () =>
      generation === sessionGeneration() &&
      context.repository === editorContext.current.repository &&
      context.workOrderId === editorContext.current.workOrderId &&
      context.taskId === editorContext.current.taskId &&
      context.image === editorContext.current.image;
    if (
      !onRemoveServer ||
      removingServerRef.current ||
      confirmedRemoval !== null
    )
      return;
    Alert.alert(
      t(
        image
          ? 'offline_clear_server_image'
          : 'offline_remove_server_attachment'
      ),
      `${file.name}\n\n${t(
        image
          ? 'offline_clear_server_image_confirm'
          : 'offline_remove_server_attachment_confirm'
      )}`,
      [
        { text: t('cancel'), style: 'cancel' },
        {
          text: t(
            image
              ? 'offline_clear_server_image'
              : 'offline_remove_server_attachment'
          ),
          style: 'destructive',
          onPress: async () => {
            if (!isCurrent() || removingServerRef.current) return;
            removingServerRef.current = true;
            setRemovingServer(true);
            try {
              assertSessionGeneration(generation);
              const result = await onRemoveServer(file.id);
              if (!isCurrent()) return;
              assertSessionGeneration(generation);
              if (result.localUpdateError) {
                setConfirmedRemoval(file.id);
                offline.reportError(result.localUpdateError);
              } else setConfirmedRemoval(null);
            } catch (error) {
              if (isCurrent()) offline.reportError(error);
            } finally {
              if (isCurrent()) {
                removingServerRef.current = false;
                setRemovingServer(false);
              }
            }
          }
        }
      ]
    );
  };
  return (
    <View>
      {files.map((file) => (
        <View key={file.id}>
          <OfflineFileItem workOrderId={workOrderId} file={file} />
          {onRemoveServer && (
            <Button
              disabled={
                !offline.networkAvailable ||
                !offline.writable ||
                removingServer ||
                confirmedRemoval !== null
              }
              onPress={() => removeFromServer(file)}
            >
              {t(
                image
                  ? 'offline_clear_server_image'
                  : 'offline_remove_server_attachment'
              )}
            </Button>
          )}
        </View>
      ))}
      {confirmedRemoval !== null && (
        <View>
          <Text accessibilityRole="alert">
            {t('offline_server_removal_refresh_pending')}
          </Text>
          <Button
            disabled={
              !offline.networkAvailable || !offline.writable || removingServer
            }
            onPress={async () => {
              const generation = sessionGeneration();
              const context = editorContext.current;
              if (removingServerRef.current) return;
              removingServerRef.current = true;
              setRemovingServer(true);
              try {
                await offline.refresh(workOrderId);
                assertSessionGeneration(generation);
                if (
                  context.repository === editorContext.current.repository &&
                  context.workOrderId === editorContext.current.workOrderId
                )
                  setConfirmedRemoval(null);
              } catch (error) {
                if (generation === sessionGeneration())
                  offline.reportError(error);
              } finally {
                if (
                  generation === sessionGeneration() &&
                  context.repository === editorContext.current.repository &&
                  context.workOrderId === editorContext.current.workOrderId
                ) {
                  removingServerRef.current = false;
                  setRemovingServer(false);
                }
              }
            }}
          >
            {t('offline_refresh_copy')}
          </Button>
        </View>
      )}
      {editable && (
        <FileUpload
          title={title}
          description={t('upload')}
          type={image || taskId ? 'image' : 'file'}
          multiple={!image}
          maxFileSize={taskId ? 35 : 7}
          selectionOnly
          disabled={saving || !offline.writable}
          onChange={(selected) =>
            void run(() =>
              offline.importFiles(
                workOrderId,
                target,
                selected,
                (taskId ? 35 : 7) * 1024 * 1024
              )
            )
          }
        />
      )}
      {ids.map((localId) => (
        <OfflineFileItem
          key={localId}
          workOrderId={workOrderId}
          localId={localId}
          onRemove={
            editable
              ? () =>
                  run(() =>
                    offline.removeDraftFile(
                      workOrderId,
                      target,
                      localId,
                      draft.revision
                    )
                  )
              : undefined
          }
        />
      ))}
      {(saving || failed || !!draft) && (
        <Text accessibilityRole={failed || fileError ? 'alert' : undefined}>
          {t(
            saving || importing
              ? 'offline_saving'
              : failed || fileError
              ? 'offline_save_failed'
              : 'offline_draft_saved'
          )}
        </Text>
      )}
      {showSubmit && editable && (
        <Button
          disabled={
            saving ||
            !filesReady ||
            !offline.writable ||
            !draft ||
            (!draft.fileIds.length && !draft.imageFileId)
          }
          onPress={() =>
            void run(() =>
              offline.submitAttachmentDraft(workOrderId, draft.revision, taskId)
            )
          }
        >
          {t('offline_save_files')}
        </Button>
      )}
    </View>
  );
}
