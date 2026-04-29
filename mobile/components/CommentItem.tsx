import React, { useState, useEffect, ReactNode } from 'react';
import {
  View,
  Text,
  TouchableOpacity,
  Image,
  ActivityIndicator,
  Alert,
  ScrollView
} from 'react-native';
import {
  Avatar,
  IconButton,
  useTheme,
  Button,
  TextInput
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from '../store';
import { deleteComment, updateComment } from '../slices/comment';
import File from '../models/file';
import mime from 'mime';
import { CompanySettingsContext } from '../contexts/CompanySettingsContext';
import useAuth from '../hooks/useAuth';
import { useNavigation } from '@react-navigation/native';
import Comment from '../models/comment';

interface CommentItemProps {
  comment: Comment;
  workOrderId: number;
  highlighted?: boolean;
}

const isImage = (file: File) => mime.getType(file.name)?.startsWith('image/');

export default function CommentItem({
  comment,
  workOrderId,
  highlighted = false
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

  const isOwner = comment.user?.id === user?.id;
  const isSystem = comment.system;

  const handleDelete = () => {
    Alert.alert(t('confirmation'), t('confirm_delete_comment'), [
      { text: t('cancel'), style: 'cancel' },
      {
        text: t('delete'),
        style: 'destructive',
        onPress: () => dispatch(deleteComment(comment.id, workOrderId))
      }
    ]);
  };

  const handleUpdate = () => {
    if (editContent && editContent.trim()) {
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
        .then(() => {
          setIsEditing(false);
          setSaving(false);
        })
        .catch(() => setSaving(false));
    }
  };

  const imageFiles = comment.files?.filter(isImage) || [];
  const otherFiles = comment.files?.filter((f) => !isImage(f)) || [];

  const renderContentWithMentions = (content: string) => {
    const mentionRegex = /@\[([^\]]+)\]\(user:(\d+)\)/g;
    const parts: ReactNode[] = [];
    let lastIndex = 0;
    let match;

    while ((match = mentionRegex.exec(content)) !== null) {
      if (match.index > lastIndex) {
        parts.push(content.slice(lastIndex, match.index));
      }
      const [, displayName, userId] = match;
      parts.push(
        <Text
          key={`mention-${match.index}`}
          style={{ color: theme.colors.primary, fontWeight: '600' }}
          onPress={() =>
            navigation.navigate('UserDetails', { id: Number(userId) })
          }
        >
          @{displayName}
        </Text>
      );
      lastIndex = match.index + match[0].length;
    }

    if (lastIndex < content.length) {
      parts.push(content.slice(lastIndex));
    }

    return parts.length > 0 ? parts : content;
  };

  return (
    <View
      style={{
        padding: 12,
        borderRadius: 8,
        backgroundColor: highlighted
          ? theme.colors.primaryContainer
          : theme.colors.surface,
        borderWidth: highlighted ? 2 : 1,
        borderColor: highlighted ? theme.colors.primary : theme.colors.outline,
        marginBottom: 8
      }}
    >
      <View style={{ flexDirection: 'row', alignItems: 'flex-start' }}>
        <TouchableOpacity
          onPress={() =>
            navigation.navigate('UserDetails', { id: comment.user?.id })
          }
        >
          {comment.user.image ? (
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
            <View>
              <TouchableOpacity
                onPress={() =>
                  navigation.navigate('UserDetails', { id: comment.user?.id })
                }
              >
                <Text style={{ fontWeight: 'bold' }}>
                  {`${comment.user?.firstName || ''} ${
                    comment.user?.lastName || ''
                  }`}
                </Text>
              </TouchableOpacity>
              <Text
                style={{ fontSize: 12, color: theme.colors.onSurfaceVariant }}
              >
                {getFormattedDate
                  ? getFormattedDate(comment.createdAt)
                  : comment.createdAt}
              </Text>
            </View>
            {!isSystem && isOwner && !isEditing && (
              <View style={{ flexDirection: 'row' }}>
                <IconButton
                  icon="pencil"
                  size={20}
                  onPress={() => setIsEditing(true)}
                />
                <IconButton icon="delete" size={20} onPress={handleDelete} />
              </View>
            )}
          </View>

          {isEditing ? (
            <View>
              <TextInput
                value={editContent}
                onChangeText={setEditContent}
                multiline
                mode="outlined"
                style={{ minHeight: 60 }}
              />
              <View style={{ flexDirection: 'row', marginTop: 8, gap: 8 }}>
                <Button
                  mode="contained"
                  onPress={handleUpdate}
                  disabled={!editContent?.trim() || saving}
                >
                  {saving ? (
                    <ActivityIndicator size="small" color="white" />
                  ) : (
                    t('save')
                  )}
                </Button>
                <Button mode="outlined" onPress={() => setIsEditing(false)}>
                  {t('cancel')}
                </Button>
              </View>
            </View>
          ) : (
            <Text
              style={{
                marginTop: 4,
                color: comment.system
                  ? theme.colors.onSurfaceVariant
                  : undefined
              }}
            >
              {renderContentWithMentions(comment.content)}
            </Text>
          )}

          {comment.files?.length > 0 && !isEditing && (
            <View style={{ marginTop: 8 }}>
              {imageFiles.length > 0 && (
                <ScrollView
                  horizontal
                  style={{ marginBottom: otherFiles.length > 0 ? 8 : 0 }}
                >
                  {imageFiles.map((file) => (
                    <TouchableOpacity key={file.id} onPress={() => {}}>
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
                    padding: 8,
                    backgroundColor: theme.colors.surfaceVariant,
                    borderRadius: 4,
                    marginBottom: 4
                  }}
                  onPress={() => {}}
                >
                  <IconButton icon="file" size={20} />
                  <Text style={{ flex: 1 }} numberOfLines={1}>
                    {file.name}
                  </Text>
                </TouchableOpacity>
              ))}
            </View>
          )}
        </View>
      </View>
    </View>
  );
}
