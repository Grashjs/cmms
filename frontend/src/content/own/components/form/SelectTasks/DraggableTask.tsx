import * as React from 'react';
import { useEffect, useState } from 'react';
import { Draggable } from 'react-beautiful-dnd';

import {
  Box,
  Button,
  Collapse,
  IconButton,
  ListItem,
  Menu,
  MenuItem,
  Select,
  TextField
} from '@mui/material';
import DragIndicatorTwoToneIcon from '@mui/icons-material/DragIndicatorTwoTone';
import { Task, TaskOption, TaskType } from '../../../../../models/owns/tasks';
import { useTranslation } from 'react-i18next';
import DoDisturbOnTwoToneIcon from '@mui/icons-material/DoDisturbOnTwoTone';
import MoreVertTwoToneIcon from '@mui/icons-material/MoreVertTwoTone';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import { randomInt } from '../../../../../utils/generators';
import { AssetMiniDTO } from '../../../../../models/owns/asset';
import { UserMiniDTO } from '../../../../../models/user';
import { MeterMiniDTO } from 'src/models/owns/meter';

export type DraggableListItemProps = {
  task: Task;
  index: number;
  onLabelChange: (value: string, id: number) => void;
  onTypeChange: (value: TaskType, id: number) => void;
  onUserChange: (user: UserMiniDTO, id: number) => void;
  onAssetChange: (asset: AssetMiniDTO, id: number) => void;
  onMeterChange: (meter: MeterMiniDTO, id: number) => void;
  onChoicesChange: (choices: string[], id: number) => void;
  onRemove: (id: number) => void;
  assetsMini: AssetMiniDTO[];
  usersMini: UserMiniDTO[];
  metersMini: MeterMiniDTO[];
};

const DraggableListItem = ({
                             task,
                             index,
                             onLabelChange,
                             onTypeChange,
                             onRemove,
                             onUserChange,
                             onAssetChange,
                             onMeterChange,
                             onChoicesChange,
                             assetsMini,
                             usersMini,
                             metersMini
                           }: DraggableListItemProps) => {
  const { t }: { t: any } = useTranslation();
  const [anchorEl, setAnchorEl] = React.useState<null | HTMLElement>(null);
  const open = Boolean(anchorEl);

  const handleOpenMenu = (event: React.MouseEvent<HTMLButtonElement>) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };
  const taskTypes = [
    { label: t('sub_task_status'), value: 'SUBTASK' },
    { label: t('text_field'), value: 'TEXT' },
    { label: t('number_field'), value: 'NUMBER' },
    { label: t('inspection_check'), value: 'INSPECTION' },
    { label: t('multiple_choices'), value: 'MULTIPLE' },
    { label: t('meter_reading'), value: 'METER' }
  ];
  const [openAssignUser, setOpenAssignUser] = useState<boolean>(
    !!task.taskBase.user
  );
  const [openAssignAsset, setOpenAssignAsset] = useState<boolean>(
    !!task.taskBase.asset
  );
  const [openAssignMeter, setOpenAssignMeter] = useState<boolean>(
    !!task.taskBase.meter
  );
  const [choices, setChoices] = useState<TaskOption[]>(
    task.taskBase.options ?? [
      { id: randomInt(), label: '' },
      { id: randomInt(), label: '' }
    ]
  );
  const handleChoiceChange = (value: string, index: number) => {
    const newChoices = [...choices];
    newChoices[index] = { id: newChoices[index].id, label: value };
    setChoices(newChoices);
  };
  const handleAddOption = () => {
    const newChoices = [...choices, { id: randomInt(), label: '' }];
    setChoices(newChoices);
  };
  const handleRemoveOption = (id: number) => {
    const newChoices = [...choices];
    newChoices.splice(id, 1);
    setChoices(newChoices);
  };
  useEffect(
    () =>
      onChoicesChange(
        choices.map((choice) => choice.label),
        task.id
      ),
    [choices]
  );

  const renderMenu = () => (
    <Menu
      id="basic-menu"
      anchorEl={anchorEl}
      open={open}
      onClose={handleClose}
      MenuListProps={{
        'aria-labelledby': 'basic-button'
      }}
    >
      <MenuItem onClick={() => setOpenAssignUser(!openAssignUser)}>
        {openAssignUser && <CheckTwoToneIcon />}
        {t('assign_user')}
      </MenuItem>
      <MenuItem onClick={() => setOpenAssignAsset(!openAssignAsset)}>
        {openAssignAsset && <CheckTwoToneIcon />}
        {t('assign_asset')}
      </MenuItem>
    </Menu>
  );

  return (
    <Draggable draggableId={task.id.toString()} index={index}>
      {(provided, snapshot) => (
        <ListItem
          ref={provided.innerRef}
          {...provided.draggableProps}
          {...provided.dragHandleProps}
          sx={{
            width: '100%',
            ...(snapshot.isDragging && { background: 'rgb(235,235,235)' })
          }}
        >
          {renderMenu()}
          <Box sx={{ display: 'flex', flexDirection: 'row', width: '100%' }}>
            <DragIndicatorTwoToneIcon />
            <Box
              sx={{ display: 'flex', flexDirection: 'column', width: '100%' }}
            >
              <Box
                sx={{
                  display: 'flex',
                  flexDirection: 'row',
                  justifyContent: 'space-between'
                }}
              >
                <Box sx={{ alignItems: 'center' }}>
                  <TextField
                    onChange={(event) =>
                      onLabelChange(event.target.value, task.id)
                    }
                    value={task.taskBase.label}
                  />
                </Box>
                <Select
                  sx={{ ml: 1 }}
                  value={task.taskBase.taskType}
                  onChange={(event) => {
                    if (event.target.value === 'METER') {
                      setOpenAssignMeter(true);
                    }
                    onTypeChange(event.target.value as TaskType, task.id);
                  }}
                >
                  {taskTypes.map((taskType) => (
                    <MenuItem key={taskType.value} value={taskType.value}>
                      {taskType.label}
                    </MenuItem>
                  ))}
                </Select>
                <Box>
                  <IconButton onClick={() => onRemove(task.id)}>
                    <DoDisturbOnTwoToneIcon color="error" />
                  </IconButton>
                  <IconButton onClick={handleOpenMenu}>
                    <MoreVertTwoToneIcon />
                  </IconButton>
                </Box>
              </Box>
              <Collapse
                in={
                  openAssignUser ||
                  openAssignAsset ||
                  openAssignMeter ||
                  task.taskBase.taskType === 'MULTIPLE'
                }
              >
                <Box
                  sx={{
                    display: 'flex',
                    flexDirection: 'column',
                    ml: 2
                  }}
                >
                  {openAssignUser && (
                    <Select
                      sx={{ mt: 1 }}
                      onChange={(event) =>
                        onUserChange(
                          usersMini.find(
                            (user) => user.id === Number(event.target.value)
                          ),
                          task.id
                        )
                      }
                      displayEmpty
                      defaultValue=""
                      value={task.taskBase.user?.id ?? ''}
                    >
                      <MenuItem value="">{t('select_user')}</MenuItem>
                      {usersMini.map((user) => (
                        <MenuItem
                          key={user.id}
                          value={user.id}
                        >{`${user.firstName} ${user.lastName}`}</MenuItem>
                      ))}
                    </Select>
                  )}
                  {openAssignAsset && (
                    <Select
                      sx={{ mt: 1 }}
                      onChange={(event) =>
                        onAssetChange(
                          assetsMini.find(
                            (asset) => asset.id === Number(event.target.value)
                          ),
                          task.id
                        )
                      }
                      displayEmpty
                      defaultValue=""
                      value={task.taskBase.asset?.id ?? ''}
                    >
                      <MenuItem value="">{t('select_asset')}</MenuItem>
                      {assetsMini.map((asset) => (
                        <MenuItem key={asset.id} value={asset.id}>
                          {asset.name}
                        </MenuItem>
                      ))}
                    </Select>
                  )}
                  {openAssignMeter && task.taskBase.taskType === 'METER' && (
                    <Select
                      sx={{ mt: 1 }}
                      onChange={(event) =>
                        onMeterChange(
                          metersMini.find(
                            (meter) => meter.id === Number(event.target.value)
                          ),
                          task.id
                        )
                      }
                      displayEmpty
                      defaultValue=""
                      value={task.taskBase.meter?.id ?? ''}
                    >
                      <MenuItem value="">{t('select_meter')}</MenuItem>
                      {metersMini.map((meter) => (
                        <MenuItem key={meter.id} value={meter.id}>
                          {meter.name}
                        </MenuItem>
                      ))}
                    </Select>
                  )}
                  {task.taskBase.taskType === 'MULTIPLE' && (
                    <Box>
                      {choices.map((choice, index) => (
                        <Box
                          key={index}
                          sx={{
                            display: 'flex',
                            flexDirection: 'row',
                            alignItems: 'center',
                            width: '100%',
                            mt: 1
                          }}
                        >
                          <TextField
                            value={choice.label}
                            onChange={(event) =>
                              handleChoiceChange(event.target.value, index)
                            }
                          />
                          {choices.length > 2 && (
                            <IconButton
                              sx={{ ml: 2 }}
                              onClick={() => handleRemoveOption(index)}
                            >
                              <DoDisturbOnTwoToneIcon color="error" />
                            </IconButton>
                          )}
                        </Box>
                      ))}
                      <Button
                        onClick={handleAddOption}
                        startIcon={<AddTwoToneIcon />}
                        sx={{ mt: 1 }}
                      >
                        {t('add_new_option')}
                      </Button>
                    </Box>
                  )}
                </Box>
              </Collapse>
            </Box>
          </Box>
        </ListItem>
      )}
    </Draggable>
  );
};

export default DraggableListItem;
