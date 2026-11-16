import { Task, TaskOption, TaskType } from '../models/tasks';
import { View } from './Themed';
import {
  Button,
  IconButton,
  Text,
  TextInput,
  useTheme
} from 'react-native-paper';
import { useTranslation } from 'react-i18next';
import { useEffect, useMemo, useRef, useState } from 'react';
import useAuth from '../hooks/useAuth';
import debounce from 'lodash.debounce';
import { SheetManager } from 'react-native-actions-sheet';
import { PermissionEntity } from '../models/role';
import { PlanFeature } from '../models/subscriptionPlan';
import { Image, TouchableOpacity } from 'react-native';
import type { ReactNode } from 'react';

interface SingleTaskProps {
  task: Task;
  imagesEditor?: ReactNode;
  connectedDisabled?: boolean;
  saving?: boolean;
  preview?: boolean;
  handleChange?: (value: string | number, id: number) => Promise<boolean> | void;
  localCommitOnSave?: boolean;
  handleSaveNotes?: (value: string, id: number) => Promise<void>;
  handleNoteChange?: (value: string, id: number) => void;
  handleSelectImages?: (id: number) => void;
  handleZoomImage?: (images: string[], image: string) => void;
  onInputFocus?: (target: number) => void;
  toggleNotes?: (id: number) => void;
  notes?: Map<number, boolean>;
}

export default function SingleTask({
  task,
  imagesEditor,
  connectedDisabled = false,
  saving = false,
  handleChange,
  localCommitOnSave = false,
  handleNoteChange,
  handleSaveNotes,
  preview,
  toggleNotes,
  notes,
  handleSelectImages,
  handleZoomImage,
  onInputFocus
}: SingleTaskProps) {
  const theme = useTheme();
  const { t }: { t: any } = useTranslation();
  const [savingNotes, setSavingNotes] = useState<boolean>(false);
  const notesVisible = !!notes?.get(task.id);
  const { user, hasCreatePermission, hasFeature } = useAuth();
  const [inputValue, setInputValue] = useState<string>(
    task.value?.toString() || ''
  );
  const valueInput = useRef<any>(null);
  const inputRevision = useRef(0);
  const changeHandler = (newValue: string) => {
    if (!preview) {
      let formattedValue = newValue;
      if (
        task.taskBase.taskType === 'METER' ||
        task.taskBase.taskType === 'NUMBER'
      ) {
        formattedValue =
          newValue?.replace(/[^0-9.]/g, '').replace(/(\..*)\./g, '$1') ?? '';
        setInputValue(formattedValue);
      } else setInputValue(formattedValue);
      if (localCommitOnSave) {
        editingText.current = true;
        if (formattedValue === '') {
          debouncedChangeHandler.cancel();
          return;
        }
        const revision = ++inputRevision.current;
        debouncedChangeHandler(formattedValue, revision);
      } else if (formattedValue !== '') handleChange(formattedValue, task.id);
    }
  };

  const editingText = useRef(false);
  const changeHandlerRef = useRef((value: string) => handleChange?.(value, task.id));
  changeHandlerRef.current = (value: string) => handleChange?.(value, task.id);
  const debouncedChangeHandler = useMemo(
    () =>
      debounce((value: string, revision: number) => {
        editingText.current = false;
        void Promise.resolve(changeHandlerRef.current(value)).then((saved) => {
          if (localCommitOnSave && saved && revision === inputRevision.current)
            valueInput.current?.blur();
        });
      }, 1000),
    []
  );
  useEffect(
    () => () => debouncedChangeHandler.cancel(),
    [debouncedChangeHandler]
  );

  useEffect(() => {
    if (!editingText.current) setInputValue(task.value?.toString() || '');
  }, [task.value]);

  const onDropdownValueChange = (value) => {
    !preview &&
      !(task.taskBase.user && task.taskBase.user.id !== user.id) &&
      handleChange(value, task.id);
  };

  const subtaskOptions = ['OPEN', 'IN_PROGRESS', 'ON_HOLD', 'COMPLETE'];
  const inspectionOptions = ['PASS', 'FLAG', 'FAIL'];

  const getOptions = (type: TaskType, options: TaskOption[]) => {
    switch (type) {
      case 'SUBTASK':
        return subtaskOptions.map((status) => ({
          value: status,
          label: t(status)
        }));
      case 'INSPECTION':
        return inspectionOptions.map((option) => ({
          value: option,
          label: t(option)
        }));
      case 'MULTIPLE':
        return options
          .map((option) => option.label)
          .map((option) => {
            return {
              label: option,
              value: option
            };
          });
      default:
        break;
    }
  };
  return (
    <View style={{ marginBottom: 10 }}>
      <View
        style={{
          display: 'flex',
          flexDirection: 'row',
          justifyContent: 'space-between',
          alignItems: 'center'
        }}
      >
        <Text variant="titleMedium" style={{ width: '70%' }}>
          {task.taskBase.label || `<${t('enter_task_name')}>`}
        </Text>
        <View style={{ display: 'flex', flexDirection: 'row' }}>
          <IconButton
            iconColor={theme.colors.primary}
            icon={'image'}
            onPress={() => handleSelectImages(task.id)}
            disabled={
              connectedDisabled ||
              preview ||
              !(
                hasCreatePermission(PermissionEntity.FILES) &&
                hasFeature(PlanFeature.FILE)
              )
            }
          />
          <IconButton
            iconColor={theme.colors.primary}
            icon={'note-outline'}
            onPress={() => !preview && toggleNotes(task.id)}
          />
        </View>
      </View>
      {['SUBTASK', 'INSPECTION', 'MULTIPLE'].includes(
        task.taskBase.taskType
      ) ? (
        <TouchableOpacity
          disabled={connectedDisabled || saving}
          onPress={() => {
            SheetManager.show('dropdown-sheet', {
              payload: {
                value: preview
                  ? getOptions(task.taskBase.taskType, task.taskBase.options)[0]
                      .value
                  : task.value,
                items: getOptions(
                  task.taskBase.taskType,
                  task.taskBase.options
                ),
                setValue: onDropdownValueChange
              }
            });
          }}
        >
          <View pointerEvents="none">
            <TextInput
              editable={false}
              value={
                getOptions(task.taskBase.taskType, task.taskBase.options).find(
                  (o) =>
                    o.value ===
                    (preview
                      ? getOptions(
                          task.taskBase.taskType,
                          task.taskBase.options
                        )[0].value
                      : task.value)
                )?.label
              }
              mode="outlined"
              right={<TextInput.Icon icon="menu-down" />}
            />
          </View>
        </TouchableOpacity>
      ) : task.taskBase.taskType === 'METER' ||
        task.taskBase.taskType === 'NUMBER' ? (
        <TextInput
          ref={valueInput}
          onFocus={(event) => onInputFocus?.(event.nativeEvent.target)}
          defaultValue={task.value?.toString()}
          onChangeText={changeHandler}
          onBlur={() => localCommitOnSave && debouncedChangeHandler.flush()}
          label={t('value')}
          value={inputValue}
          mode={'outlined'}
          keyboardType="decimal-pad"
          disabled={
            connectedDisabled ||
            saving ||
            (task.taskBase.user && task.taskBase.user.id !== user.id)
          }
        />
      ) : (
        <TextInput
          ref={valueInput}
          onFocus={(event) => onInputFocus?.(event.nativeEvent.target)}
          value={inputValue}
          onChangeText={(text) => {
            editingText.current = true;
            setInputValue(text);
            debouncedChangeHandler(text, ++inputRevision.current);
          }}
          onBlur={() => debouncedChangeHandler.flush()}
          label={t('value')}
          mode={'outlined'}
          disabled={
            connectedDisabled ||
            saving ||
            (task.taskBase.user && task.taskBase.user.id !== user.id)
          }
        />
      )}
      {task.taskBase.asset && (
        <View
          style={{
            marginVertical: 10,
            display: 'flex',
            flexDirection: 'row',
            justifyContent: 'space-between'
          }}
        >
          <Text style={{ fontWeight: 'bold' }}>{t('concerned_asset')}</Text>
          <TouchableOpacity>
            <Text style={{ color: theme.colors.primary }}>
              {task.taskBase.asset.name}
            </Text>
          </TouchableOpacity>
        </View>
      )}
      {task.taskBase.user && (
        <View
          style={{
            marginVertical: 10,
            display: 'flex',
            flexDirection: 'row',
            justifyContent: 'space-between'
          }}
        >
          <Text style={{ fontWeight: 'bold' }}>{t('assigned_to')}</Text>
          <TouchableOpacity>
            <Text
              style={{ color: theme.colors.primary }}
            >{`${task.taskBase.user.firstName} ${task.taskBase.user.lastName}`}</Text>
          </TouchableOpacity>
        </View>
      )}
      {notesVisible && (
        <View style={{ display: notesVisible ? 'flex' : 'none' }}>
          <TextInput
            mode={'outlined'}
            multiline
            onFocus={(event) => onInputFocus?.(event.nativeEvent.target)}
            disabled={connectedDisabled}
            value={task.notes}
            label={t('notes')}
            onChangeText={(value) =>
              !preview && handleNoteChange(value, task.id)
            }
          />
          <Button
            style={{ marginTop: 10 }}
            mode="contained"
            loading={savingNotes}
            disabled={savingNotes || saving || connectedDisabled}
            onPress={() => {
              setSavingNotes(true);
              handleSaveNotes(task.notes, task.id).finally(() =>
                setSavingNotes(false)
              );
            }}
          >
            {t('save')}
          </Button>
        </View>
      )}
      {imagesEditor}
      {!imagesEditor && connectedDisabled && !!task.images?.length && (
        <Text>{t('offline_files_not_downloaded')}</Text>
      )}
      {!imagesEditor &&
        !connectedDisabled &&
        task.images?.map((image) => (
          <TouchableOpacity
            key={image.id}
            onPress={() =>
              handleZoomImage(
                task.images.map((img) => img.url),
                image.url
              )
            }
            style={{ marginTop: 10 }}
          >
            <Image
              source={{ uri: image.url }}
              style={{ borderRadius: 5, height: 150 }}
            />
          </TouchableOpacity>
        ))}
    </View>
  );
}
