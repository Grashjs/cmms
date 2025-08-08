import {
  Autocomplete,
  Box,
  Card,
  IconButton,
  InputAdornment,
  Link,
  TextField,
  Typography
} from '@mui/material';
import { FormikProps, useFormikContext } from 'formik';
import { useState } from 'react';
import { getLocationChildren, getLocationsMini } from 'src/slices/location';
import { IField, IHash } from '../../type';
import SelectAssetModal from './SelectAssetModal'; // Import the new modal
import SelectForm from './SelectForm';
import SelectParts from './SelectParts';
import { useDispatch, useSelector } from '../../../../store';
import SearchIcon from '@mui/icons-material/Search'; // Added SearchIcon
import SelectTasksModal from './SelectTasks';
import { getCustomersMini } from '../../../../slices/customer';
import { getVendorsMini } from '../../../../slices/vendor';
import { getUsersMini } from '../../../../slices/user';
import { getAssetsMini } from '../../../../slices/asset';
import { getTeamsMini } from '../../../../slices/team';
import AssignmentTwoToneIcon from '@mui/icons-material/AssignmentTwoTone';
import EditTwoToneIcon from '@mui/icons-material/EditTwoTone';
import AddCircleTwoToneIcon from '@mui/icons-material/AddCircleTwoTone';
import { getPriorityLabel } from '../../../../utils/formatters';
import { getCategories } from '../../../../slices/category';
import { getRoles } from '../../../../slices/role';
import { getCurrencies } from '../../../../slices/currency';
import ClearTwoToneIcon from '@mui/icons-material/ClearTwoTone';
import { useTranslation } from 'react-i18next';
import SelectLocationModal from './SelectLocationModal';

export const CustomSelect = ({
  field,
  handleChange
}: {
  field: IField;
  handleChange: (formik: FormikProps<IHash<any>>, field: string, e) => void;
}) => {
  const [assetModalOpen, setAssetModalOpen] = useState(false);
  const [locationModalOpen, setLocationModalOpen] = useState(false);
  const { t } = useTranslation();
  const formik = useFormikContext();
  const [openTask, setOpenTask] = useState(false);
  const dispatch = useDispatch();
  const { customersMini } = useSelector((state) => state.customers);
  const { vendorsMini } = useSelector((state) => state.vendors);
  const { locationsMini, locationsHierarchy } = useSelector(
    (state) => state.locations
  );
  const { categories } = useSelector((state) => state.categories);
  const { usersMini } = useSelector((state) => state.users);
  const { assetsMini } = useSelector((state) => state.assets);
  const { teamsMini } = useSelector((state) => state.teams);
  const { roles } = useSelector((state) => state.roles);
  const { currencies } = useSelector((state) => state.currencies);

  const fetchCustomers = async () => {
    dispatch(getCustomersMini());
  };

  const fetchVendors = async () => {
    dispatch(getVendorsMini());
  };
  const fetchUsers = async () => {
    dispatch(getUsersMini());
  };
  const fetchLocations = async () => {
    dispatch(getLocationsMini());
  };
  const fetchRoles = async () => {
    dispatch(getRoles());
  };

  const fetchCategories = async (category: string) => {
    dispatch(getCategories(category));
  };
  const fetchAssets = async (locationId: number) => {
    dispatch(getAssetsMini(locationId));
  };
  const fetchTeams = async () => {
    dispatch(getTeamsMini());
  };
  const fetchCurrencies = async () => {
    if (!currencies.length) dispatch(getCurrencies());
  };

  let options = field.items;
  let loading = field.loading;
  let onOpen = field.onPress;
  let fieldValue = formik.values[field.name];
  const excluded = field.excluded;

  switch (field.type2) {
    case 'customer':
      options = customersMini.map((customer) => {
        return {
          label: customer.name,
          value: customer.id
        };
      });
      onOpen = fetchCustomers;
      break;
    case 'vendor':
      options = vendorsMini.map((vendor) => {
        return {
          label: vendor.companyName,
          value: vendor.id
        };
      });
      onOpen = fetchVendors;
      break;
    case 'user':
      options = usersMini.map((user) => {
        return {
          label: `${user.firstName} ${user.lastName}`,
          value: user.id
        };
      });
      onOpen = fetchUsers;
      break;
    case 'team':
      options = teamsMini.map((team) => {
        return {
          label: team.name,
          value: team.id
        };
      });
      onOpen = fetchTeams;
      break;
    case 'currency':
      options = currencies.map((currency) => {
        return {
          label: currency.name,
          value: currency.id
        };
      });
      onOpen = fetchCurrencies;
      break;
    case 'location':
    case 'parentLocation':
      options = locationsMini
        .filter((location) => location.id !== excluded)
        .map((location) => {
          return {
            label: location.name,
            value: location.id
          };
        });
      onOpen = fetchLocations;

      return (
        <>
          <Autocomplete
            renderInput={(params) => (
              <TextField
                {...params}
                fullWidth={field.fullWidth || true}
                variant="outlined"
                required={field.required}
                label={field.label}
                placeholder={field.placeholder}
                error={!!formik.errors[field.name] || field.error}
                helperText={formik.errors[field.name]}
                InputProps={{
                  ...params.InputProps,
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation(); // Prevent TextField onClick from firing again
                          setLocationModalOpen(true);
                        }}
                      >
                        <SearchIcon />
                      </IconButton>
                    </InputAdornment>
                  )
                }}
              />
            )}
            fullWidth={field.fullWidth || true}
            disabled={formik.isSubmitting}
            onOpen={onOpen}
            key={field.name}
            //@ts-ignore
            getOptionLabel={(option) => option.label}
            isOptionEqualToValue={(option, value) =>
              //@ts-ignore
              option.value == value.value
            }
            multiple={field.multiple}
            value={field.multiple ? fieldValue ?? [] : fieldValue ?? null}
            options={options}
            onChange={(event, newValue) => {
              handleChange(formik, field.name, newValue);
            }}
          />
          <SelectLocationModal
            open={locationModalOpen}
            onClose={() => setLocationModalOpen(false)}
            excludedLocationIds={[excluded]}
            maxSelections={field.multiple ? 10 : 1}
            onSelect={(selectedLocations) => {
              handleChange(
                formik,
                field.name,
                field.multiple
                  ? selectedLocations.map((location) => ({
                      label: location.name,
                      value: location.id
                    }))
                  : selectedLocations.length
                  ? {
                      label: selectedLocations[0].name,
                      value: selectedLocations[0].id
                    }
                  : null
              );
              setLocationModalOpen(false); // Close the modal
            }}
            initialSelectedLocations={locationsMini.filter((location) =>
              (field.multiple
                ? fieldValue ?? []
                : fieldValue
                ? [fieldValue]
                : []
              ).some((a) => Number(a.value) === location.id)
            )}
          />
        </>
      );
    case 'asset': {
      options = assetsMini
        .filter((asset) => asset.id !== excluded)
        .map((asset) => {
          return {
            label: asset.name,
            value: asset.id
          };
        });
      const locationId = field.relatedFields?.length
        ? formik.values[field.relatedFields[0].field]?.value ?? null
        : null;
      onOpen = () => fetchAssets(locationId || null);

      return (
        <>
          <Autocomplete
            renderInput={(params) => (
              <TextField
                {...params}
                fullWidth={field.fullWidth || true}
                variant="outlined"
                required={field.required}
                label={field.label}
                placeholder={field.placeholder}
                error={!!formik.errors[field.name] || field.error}
                helperText={formik.errors[field.name]}
                InputProps={{
                  ...params.InputProps,
                  endAdornment: (
                    <InputAdornment position="end">
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation(); // Prevent TextField onClick from firing again
                          setAssetModalOpen(true);
                        }}
                      >
                        <SearchIcon />
                      </IconButton>
                    </InputAdornment>
                  )
                }}
              />
            )}
            fullWidth={field.fullWidth || true}
            disabled={formik.isSubmitting}
            onOpen={onOpen}
            key={field.name}
            //@ts-ignore
            getOptionLabel={(option) => option.label}
            isOptionEqualToValue={(option, value) =>
              //@ts-ignore
              option.value == value.value
            }
            multiple={field.multiple}
            value={field.multiple ? fieldValue ?? [] : fieldValue ?? null}
            options={options}
            onChange={(event, newValue) => {
              handleChange(formik, field.name, newValue);
            }}
          />
          <SelectAssetModal
            open={assetModalOpen}
            onClose={() => setAssetModalOpen(false)}
            excludedAssetIds={[excluded]}
            locationId={locationId}
            maxSelections={field.multiple ? 10 : 1}
            onSelect={(selectedAssets) => {
              handleChange(
                formik,
                field.name,
                field.multiple
                  ? selectedAssets.map((asset) => ({
                      label: asset.name,
                      value: asset.id
                    }))
                  : selectedAssets.length
                  ? {
                      label: selectedAssets[0].name,
                      value: selectedAssets[0].id
                    }
                  : null
              );
              setAssetModalOpen(false); // Close the modal
            }}
            initialSelectedAssets={assetsMini.filter((asset) =>
              (field.multiple
                ? fieldValue ?? []
                : fieldValue
                ? [fieldValue]
                : []
              ).some((a) => Number(a.value) === asset.id)
            )}
          />
        </>
      );
    }
    case 'role':
      options = roles.map((role) => {
        return {
          label: role.name,
          value: role.id
        };
      });
      onOpen = fetchRoles;
      break;
    case 'category':
      options =
        categories[field.category]?.map((category) => {
          return {
            label: category.name,
            value: category.id
          };
        }) ?? [];
      onOpen = () => fetchCategories(field.category);
      break;
    case 'priority':
      options = ['NONE', 'LOW', 'MEDIUM', 'HIGH'].map((value) => {
        return {
          label: getPriorityLabel(value, t),
          value
        };
      });
      break;
    case 'part':
      return (
        <>
          <Box display="flex" flexDirection="column">
            {fieldValue?.length
              ? fieldValue.map(({ label, value }) => (
                  <Link
                    sx={{ mb: 1 }}
                    target="_blank"
                    rel="noopener noreferrer"
                    href={`/app/inventory/parts/${value}`}
                    key={value}
                    variant="h4"
                  >
                    {label}
                  </Link>
                ))
              : null}
          </Box>
          <SelectParts
            selected={
              fieldValue?.map(({ label, value }) => Number(value)) ?? []
            }
            onChange={(newParts) => {
              handleChange(
                formik,
                field.name,
                newParts.map((part) => {
                  return { label: part.name, value: part.id };
                })
              );
            }}
          />
        </>
      );
    case 'task':
      return (
        <>
          <SelectTasksModal
            open={openTask}
            onClose={() => setOpenTask(false)}
            selected={fieldValue ?? []}
            onSelect={(tasks) => {
              handleChange(formik, field.name, tasks);
              return Promise.resolve();
            }}
          />
          <Card onClick={() => setOpenTask(true)} sx={{ cursor: 'pointer' }}>
            <Box
              sx={{
                p: 2,
                display: 'flex',
                flexDirection: 'row',
                justifyContent: 'space-between',
                alignItems: 'center'
              }}
            >
              <AssignmentTwoToneIcon />
              <Box>
                <Typography variant="h4" color="primary">
                  {fieldValue ? fieldValue.length : null} {t('tasks')}
                </Typography>
                <Typography variant="subtitle1">
                  {t('assign_tasks_description')}
                </Typography>
              </Box>
              <IconButton>
                {fieldValue?.length ? (
                  <EditTwoToneIcon color="primary" />
                ) : (
                  <AddCircleTwoToneIcon color="primary" />
                )}
              </IconButton>
            </Box>
          </Card>
        </>
      );
    default:
      break;
  }
  return (
    <SelectForm
      options={options}
      value={fieldValue}
      label={field.label}
      onChange={(e, values) => {
        handleChange(formik, field.name, values);
      }}
      disabled={formik.isSubmitting}
      loading={loading}
      required={field?.required}
      error={!!formik.errors[field.name] || field.error}
      errorMessage={formik.errors[field.name]}
      onOpen={onOpen}
      placeholder={field.placeholder}
      multiple={field.multiple}
      fullWidth={field.fullWidth}
      key={field.name}
    />
  );
};
