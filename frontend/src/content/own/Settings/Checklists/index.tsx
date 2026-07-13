import { Box, Button, Grid } from '@mui/material';
import { useTranslation } from 'react-i18next';
import SettingsLayout from '../SettingsLayout';
import CustomDataGrid from '../../components/CustomDatagrid';
import { GridEnrichedColDef } from '@mui/x-data-grid/models/colDef/gridColDef';
import {
  GridActionsCellItem,
  GridRenderCellParams,
  GridRowParams,
  GridToolbar,
  GridValueGetterParams
} from '@mui/x-data-grid';
import { Checklist } from '../../../../models/owns/checklists';
import AddTwoToneIcon from '@mui/icons-material/AddTwoTone';
import SelectTasksModal from '../../components/form/SelectTasks';
import { useContext, useEffect, useState } from 'react';
import DeleteTwoToneIcon from '@mui/icons-material/DeleteTwoTone';
import { useDispatch, useSelector } from '../../../../store';
import { CustomSnackBarContext } from '../../../../contexts/CustomSnackBarContext';
import {
  addChecklist,
  deleteChecklist,
  editChecklist,
  getChecklists
} from '../../../../slices/checklist';
import ConfirmDialog from '../../components/ConfirmDialog';
import { getTaskFromTaskBase } from '../../../../utils/formatters';
import useAuth from '../../../../hooks/useAuth';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import FeatureErrorMessage from '../../components/FeatureErrorMessage';

function Checklists() {
  const { t }: { t: any } = useTranslation();
  const [openCreateChecklist, setOpenCreateChecklist] = useState(false);
  const [openEditChecklist, setOpenEditChecklist] = useState(false);
  const [currentChecklist, setCurrentChecklist] = useState<Checklist>();
  const [openDelete, setOpenDelete] = useState<boolean>(false);
  const { hasFeature } = useAuth();
  const dispatch = useDispatch();
  const { user } = useAuth();
  const { companySettingsId } = user;
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const { checklists, loadingGet } = useSelector((state) => state.checklists);

  useEffect(() => {
    if (hasFeature(PlanFeature.CHECKLIST)) dispatch(getChecklists());
  }, []);
  const onDeleteSuccess = () => {
    showSnackBar(t('checklist_delete_success'), 'success');
  };
  const onDeleteFailure = (err) =>
    showSnackBar(t('checklist_delete_failure'), 'error');
  const onEditSuccess = () => {
    setOpenEditChecklist(false);
    showSnackBar(t('changes_saved_success'), 'success');
  };
  const onEditFailure = (err) =>
    showSnackBar(t('checklist_update_failure'), 'error');
  const handleDelete = (id: number) => {
    dispatch(deleteChecklist(id)).then(onDeleteSuccess).catch(onDeleteFailure);
    setOpenDelete(false);
  };
  const columns: GridEnrichedColDef[] = [
    {
      field: 'name',
      headerName: t('name'),
      description: t('name'),
      width: 150,
      renderCell: (params: GridRenderCellParams<string>) => (
        <Box sx={{ fontWeight: 'bold' }}>{params.value}</Box>
      )
    },
    {
      field: 'description',
      headerName: t('description'),
      description: t('description'),
      width: 300
    },
    {
      field: 'category',
      headerName: t('category'),
      description: t('category'),
      width: 150
    },
    {
      field: 'tasks',
      headerName: t('tasks'),
      description: t('tasks'),
      valueGetter: (params: GridValueGetterParams<null, Checklist>) =>
        params.row.taskBases.length,
      width: 150
    },
    {
      field: 'actions',
      type: 'actions',
      headerName: t('actions'),
      description: t('actions'),
      getActions: (params: GridRowParams<Checklist>) => [
        <GridActionsCellItem
          key="delete"
          icon={<DeleteTwoToneIcon fontSize="small" color="error" />}
          onClick={() => {
            setCurrentChecklist(params.row);
            setOpenDelete(true);
          }}
          label="Remove Checklist"
        />
      ]
    }
  ];
  return (
    <>
      {hasFeature(PlanFeature.CHECKLIST) ? (
        <>
          <Grid item xs={12}>
            <Box
              sx={{
                width: '95%',
                p: 4
              }}
            >
              <Button
                sx={{
                  mb: 2
                }}
                variant="contained"
                onClick={() => setOpenCreateChecklist(true)}
                startIcon={<AddTwoToneIcon fontSize="small" />}
              >
                {t('create_checklist')}
              </Button>
              <CustomDataGrid
                columns={columns}
                rows={checklists}
                loading={loadingGet}
                components={{
                  Toolbar: GridToolbar
                }}
                onRowClick={(params: GridRowParams<Checklist>) => {
                  setCurrentChecklist(params.row);
                  setOpenEditChecklist(true);
                }}
                initialState={{
                  columns: {
                    columnVisibilityModel: {}
                  }
                }}
              />
            </Box>
          </Grid>
          <SelectTasksModal
            open={openCreateChecklist}
            onClose={() => setOpenCreateChecklist(false)}
            selected={[]}
            onSelect={(tasks, infos): Promise<void> => {
              return new Promise<void>((res, rej) => {
                dispatch(
                  addChecklist(
                    {
                      ...infos,
                      taskBases: tasks.map((task) => {
                        return {
                          ...task.taskBase,
                          options: task.taskBase.options.map(
                            (option) => option.label
                          )
                        };
                      })
                    },
                    companySettingsId
                  )
                ).finally(res);
              });
            }}
            action="createChecklist"
          />
          <SelectTasksModal
            open={openEditChecklist}
            onClose={() => setOpenEditChecklist(false)}
            selected={
              currentChecklist?.taskBases.map((taskBase) =>
                getTaskFromTaskBase(taskBase)
              ) ?? []
            }
            onSelect={(tasks, infos) => {
              return new Promise<void>((res, rej) => {
                dispatch(
                  editChecklist(currentChecklist.id, {
                    ...infos,
                    taskBases: tasks.map((task) => {
                      return {
                        ...task.taskBase,
                        options: task.taskBase.options.map(
                          (option) => option.label
                        )
                      };
                    })
                  })
                )
                  .then(onEditSuccess)
                  .then(() => res())
                  .catch((err) => {
                    onEditFailure(err);
                    rej();
                  });
              });
            }}
            action="editChecklist"
            infos={{
              name: currentChecklist?.name,
              description: currentChecklist?.description,
              category: currentChecklist?.category
            }}
          />
          <ConfirmDialog
            open={openDelete}
            onCancel={() => {
              setOpenDelete(false);
            }}
            onConfirm={() => handleDelete(currentChecklist?.id)}
            confirmText={t('to_delete')}
            question={t('confirm_delete_checklist')}
          />
        </>
      ) : (
        <FeatureErrorMessage message="upgrade_checklist" />
      )}
    </>
  );
}

export default Checklists;
