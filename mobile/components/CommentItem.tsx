import React, { useState, useEffect, ReactNode, useRef } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  Alert,
  ScrollView,
  Pressable,
  TextInput as RNTextInput
} from 'react-native';
import { Avatar, IconButton, useTheme, Button } from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../store';
import {
  deleteComment,
  updateComment,
  ConfirmedCommentMutation
} from '../slices/comment';
import File from '../models/file';
import mime from 'mime';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';
import useAuth from '../hooks/useAuth';
import { useNavigation } from '@react-navigation/native';
import Comment from '../models/comment';
import ImageView from 'react-native-image-viewing';
import { downloadFile } from '../utils/fileDownload';
import { useMentions } from 'react-native-controlled-mentions';
import { TriggersConfig } from 'react-native-controlled-mentions/dist/types/types';

import { useOffline } from '../offline/OfflineProvider';

interface CommentItemProps {
  connectedDisabled?: boolean;
  comment: Comment;
  workOrderId: number;
  highlighted?: boolean;
  users?: { id: string; name: string }[];
}

const triggersConfig: TriggersConfig<'mention'> = {
  mention: {
    trigger: '@',
    pattern: /(@\[[^\]]+\]\(user:[^)]+\))/g,
    textStyle: { fontWeight: 'bold', color: 'blue' },
    getTriggerData: (match: string) => {
      const result = match.match(/@\[(.*?)\]\(user:(.*?)\)/);
      return {
        original: match,
        trigger: '@',
        name: result?.[1] ?? '',
        id: result?.[2] ?? ''
      };
    },
    getTriggerValue: (suggestion) =>
      `@[${suggestion.name}](user:${suggestion.id})`
  }
};

const isImage = (file: File) => mime.getType(file.name)?.startsWith('image/');

export default function CommentItem({
  comment,
  connectedDisabled = false,
  workOrderId,
  highlighted = false,
  users = []
}: CommentItemProps) {
  const { t } = useTranslation();
  const theme = useTheme();
  const dispatch = useDispatch();
  const navigation = useNavigation();
  const { getFormattedDate } = React.useContext(CompanySettingsContext);
  const { user } = useAuth();
  const { loadingDelete, loadingUpdate } = useSelector(
    (state) => state.comments
  );

  const [isEditing, setIsEditing] = useState(false);
  const [editContent, setEditContent] = useState(comment.content);
  const [saving, setSaving] = useState(false);
  const editRevision = useRef(0);
  const [localUpdateFailed, setLocalUpdateFailed] = useState(false);
  const expectedLocalComment = useRef<Comment | null>(null);
  const [deletionConfirmed, setDeletionConfirmed] = useState(false);
  const offline = useOffline();
  const showLocalUpdateFailure = (result: ConfirmedCommentMutation) => {
    if (result.localUpdateError) {
      expectedLocalComment.current = result.comment ?? null;
      setLocalUpdateFailed(true);
      offline.reportError(result.localUpdateError);
    } else {
      expectedLocalComment.current = null;
      setLocalUpdateFailed(false);
    }
  };

  useEffect(() => {
    const expected = expectedLocalComment.current;
    if (
      localUpdateFailed &&
      expected &&
      comment.id === expected.id &&
      comment.updatedAt === expected.updatedAt &&
      comment.content === expected.content &&
      JSON.stringify(comment.files.map((file) => file.id)) ===
        JSON.stringify(expected.files.map((file) => file.id))
    ) {
      expectedLocalComment.current = null;
      setLocalUpdateFailed(false);
    }
  }, [comment, localUpdateFailed]);

  const isOwner = comment.user?.id === user?.id;
  const isSystem = comment.system;
  const [imageViewerOpen, setImageViewerOpen] = useState(false);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);

  const { textInputProps, triggers } = useMentions({
    value: editContent,
    onChange: (content) => {
      editRevision.current++;
      setEditContent(content);
    },
    triggersConfig
  });

  const mentionKeyword = triggers?.mention?.keyword ?? null;
  const filteredUsers = mentionKeyword
    ? users.filter((u) =>
        u.name.toLowerCase().includes(mentionKeyword.toLowerCase())
      )
    : users;

  const handleDelete = () => {
    Alert.alert(t('confirmation'), t('confirm_delete_comment'), [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('to_delete'),
        style: 'destructive',
        onPress: () => {
          if (deletionConfirmed) return;
          setSaving(true);
          void dispatch(deleteComment(comment.id, workOrderId))
            .then((result: ConfirmedCommentMutation) => {
              setDeletionConfirmed(true);
              showLocalUpdateFailure(result);
            })
            .catch(offline.reportError)
            .finally(() => setSaving(false));
        }
      }
    ]);
  };

  const handleUpdate = () => {
    if (editContent && editContent.trim() && !saving) {
      const revision = editRevision.current;
      setSaving(true);
      dispatch(
        updateComment(
          comment.id,
          {
            content: editContent,
            files: comment.files.map((f) => ({ id: f.id }))
          },
          workOrderId
        )
      )
        .then((result: ConfirmedCommentMutation) => {
          if (editRevision.current === revision) setIsEditing(false);
          showLocalUpdateFailure(result);
        })
        .catch(offline.reportError)
        .finally(() => setSaving(false));
    }
  };

  const imageFiles = comment.files?.filter(isImage) || [];
  const otherFiles = comment.files?.filter((f) => !isImage(f)) || [];

  const renderContentWithMentions = (content: string) => {
    const mentionRegex = /@\[([^\]]+)\]\(user:(\d+)\)/g;
    const parts: ReactNode[] = [];
    let lastIndex = 0;
    let match;
    let key = 0;

    while ((match = mentionRegex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push(
          <Text key={key++}>{content.slice(lastIndex, match.index)}</Text>
        );
      }
      const [, displayName, userId] = match;
      parts.push(
        <Text
          key={`mention-${match.index}`}
          style={{ color: theme.colors.primary, fontWeight: '600' }}
          onPress={() =>
            !connectedDisabled &&
            navigation.navigate('UserDetails', { id: Number(userId) })
          }
        >
          @{displayName}
        </Text>
      );
      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < content.length) {
      parts.push(<Text key={key++}>{content.slice(lastIndex)}</Text>);
    }

    return parts.length > 0 ? <Text>{parts}</Text> : <Text>{content}</Text>;
  };

  return (
    <View
      style={{
        padding: 12,
        borderRadius: 8,
        backgroundColor: highlighted ? theme.colors.surface : 'white',
        borderWidth: highlighted ? 2 : 1,
        borderColor: highlighted ? theme.colors.primary : theme.colors.outline,
        marginBottom: 8
      }}
    >
      <View style={{ flexDirection: 'row', alignItems: 'flex-start' }}>
        <TouchableOpacity
          disabled={connectedDisabled}
          onPress={() =>
            navigation.navigate('UserDetails', { id: comment.user?.id })
          }
        >
          {comment.user?.image && !connectedDisabled ? (
            <Avatar.Image size={40} source={{ uri: comment.user.image.url }} />
          ) : (
            <Avatar.Text
              size={40}
              label={`${comment.user?.firstName?.charAt(0) || ''}${
                comment.user?.lastName?.charAt(0) || ''
              }`}
              style={{ backgroundColor: theme.colors.primary }}
            />
          )}
        </TouchableOpacity>
        <View style={{ flex: 1, marginLeft: 12 }}>
          <View
            style={{
              flexDirection: 'row',
              justifyContent: 'space-between',
              alignItems: 'center'
            }}
          >
            <View style={{ flex: 1, marginRight: 8 }}>
              <TouchableOpacity
                disabled={connectedDisabled}
                onPress={() =>
                  navigation.navigate('UserDetails', { id: comment.user?.id })
                }
              >
                <Text
                  style={{ fontWeight: 'bold', color: 'black' }}
                  numberOfLines={1}
                >
                  {`${comment.user?.firstName || ''} ${
                    comment.user?.lastName || ''
                  }`}
                </Text>
              </TouchableOpacity>
              <Text
                style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}
                numberOfLines={1}
              >
                {getFormattedDate(comment.updatedAt || comment.createdAt)}
              </Text>
            </View>
            {!connectedDisabled &&
              !isSystem &&
              isOwner &&
              !isEditing &&
              !deletionConfirmed && (
                <View style={{ flexDirection: 'row' }}>
                  <IconButton
                    icon="pencil"
                    size={20}
                    disabled={saving}
                    onPress={() => setIsEditing(true)}
                  />
                  <IconButton
                    icon="delete"
                    size={20}
                    disabled={saving}
                    onPress={handleDelete}
                  />
                </View>
              )}
          </View>

          {localUpdateFailed && (
            <Text accessibilityRole="alert">
              {t('offline_comment_saved_refresh_pending')}
            </Text>
          )}
          {isEditing ? (
            <View>
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <View style={{ flex: 1, marginBottom: 8, marginRight: 8 }}>
                  {mentionKeyword && filteredUsers.length > 0 && (
                    <View
                      style={{
                        backgroundColor: '#fff',
                        borderRadius: 8,
                        elevation: 5,
                        shadowColor: '#000',
                        shadowOffset: { width: 0, height: 2 },
                        shadowOpacity: 0.2,
                        marginBottom: 8,
                        maxHeight: 200,
                        overflow: 'hidden'
                      }}
                    >
                      {filteredUsers.map((item) => (
                        <Pressable
                          key={item.id}
                          onPress={() => {
                            triggers?.mention?.onSelect?.({
                              id: item.id,
                              name: item.name
                            });
                          }}
                          style={{
                            padding: 12,
                            borderBottomWidth: 1,
                            borderBottomColor: '#eee'
                          }}
                        >
                          <Text>{item.name}</Text>
                        </Pressable>
                      ))}
                    </View>
                  )}
                  <RNTextInput
                    multiline
                    numberOfLines={2}
                    style={{
                      borderWidth: 1,
                      borderColor: '#ccc',
                      borderRadius: 4,
                      color: 'black'
                    }}
                    {...textInputProps}
                  />
                </View>
              </View>
              <View style={{ flexDirection: 'row', marginTop: 8, gap: 8 }}>
                <Button
                  mode="contained"
                  onPress={handleUpdate}
                  disabled={!editContent?.trim() || saving}
                  style={{ maxWidth: '40%' }}
                >
                  {saving ? (
                    <ActivityIndicator size="small" color="white" />
                  ) : (
                    t('save')
                  )}
                </Button>
                <Button
                  style={{ maxWidth: '40%' }}
                  mode="outlined"
                  onPress={() => setIsEditing(false)}
                >
                  {t('cancel')}
                </Button>
              </View>
            </View>
          ) : (
            <Text
              style={{
                marginTop: 4,
                color: comment.system ? theme.colors.onSurfaceVariant : 'black'
              }}
            >
              {renderContentWithMentions(comment.content)}
            </Text>
          )}

          {connectedDisabled && !!comment.files?.length && (
            <Text>{t('offline_files_not_downloaded')}</Text>
          )}
          {!connectedDisabled && comment.files?.length > 0 && !isEditing && (
            <View style={{ marginTop: 8 }}>
              {imageFiles.length > 0 && (
                <ScrollView
                  horizontal
                  style={{ marginBottom: otherFiles.length > 0 ? 8 : 0 }}
                >
                  {imageFiles.map((file, index) => (
                    <TouchableOpacity
                      key={file.id}
                      onPress={() => {
                        setSelectedImageIndex(index);
                        setImageViewerOpen(true);
                      }}
                    >
                      <Image
                        source={{ uri: file.url }}
                        style={{
                          width: 80,
                          height: 80,
                          borderRadius: 4,
                          marginRight: 8
                        }}
                      />
                    </TouchableOpacity>
                  ))}
                </ScrollView>
              )}
              {otherFiles.map((file) => (
                <TouchableOpacity
                  key={file.id}
                  style={{
                    flexDirection: 'row',
                    alignItems: 'center',
                    padding: 2,
                    backgroundColor: theme.colors.background,
                    borderRadius: 4,
                    marginBottom: 4
                  }}
                  onPress={() => downloadFile(file.url, file.name)}
                >
                  <IconButton icon="file" size={20} />
                  <Text style={{ flex: 1 }} numberOfLines={1}>
                    {file.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          )}
          {imageViewerOpen && (
            <ImageView
              images={imageFiles.map((f) => ({ uri: f.url }))}
              imageIndex={selectedImageIndex}
              visible={imageViewerOpen}
              onRequestClose={() => setImageViewerOpen(false)}
            />
          )}
        </View>
      </View>
    </View>
  );
}
