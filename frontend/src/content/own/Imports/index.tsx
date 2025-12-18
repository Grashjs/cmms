import { Helmet } from 'react-helmet-async';
import useAuth from 'src/hooks/useAuth';
import { useTranslation } from 'react-i18next';
import { PermissionEntity } from 'src/models/owns/role';
import DownloadTwoToneIcon from '@mui/icons-material/DownloadTwoTone';
import {
  Box,
  Button,
  Card,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Grid,
  Link,
  MenuItem,
  Select,
  Stack,
  Step,
  StepLabel,
  Stepper,
  Typography
} from '@mui/material';
import PermissionErrorMessage from '../components/PermissionErrorMessage';
import { useContext, useEffect, useState } from 'react';
import FileUpload from '../components/FileUpload';
import { TitleContext } from 'src/contexts/TitleContext';
import { read, utils } from 'xlsx';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import Spreadsheet, { CellBase, Matrix } from 'react-spreadsheet';
import { arrayToAoA } from 'src/utils/overall';
import { CustomSnackBarContext } from '../../../contexts/CustomSnackBarContext';
import { getOwnHeadersConfig } from 'src/utils/states';
import InfoTwoToneIcon from '@mui/icons-material/InfoTwoTone';
import { closestMatch, distance } from 'closest-match';
import {
  ImportDTO,
  ImportKeys,
  ImportResponse
} from '../../../models/owns/imports';
import { useDispatch } from '../../../store';
import { importEntity } from '../../../slices/imports';
import { PlanFeature } from '../../../models/owns/subscriptionPlan';
import FeatureErrorMessage from '../components/FeatureErrorMessage';
import api from '../../../utils/api';
import i18n from 'i18next';
import downloadFile from 'downloadjs';

interface OwnProps {}

export interface OwnHeader {
  label: string;
  keyName: ImportKeys;
  required?: boolean;
  formatter?: (value: any) => any;
}

export type EntityType =
  | 'work-orders'
  | 'locations'
  | 'assets'
  | 'parts'
  | 'meters'
  | 'preventive-maintenances';

const Import = ({}: OwnProps) => {
  const { hasViewPermission, hasFeature } = useAuth();
  const { t }: { t: any } = useTranslation();
  const entityFromUrl = window.location.href.substring(
    window.location.href.lastIndexOf('/') + 1
  );
  const [entity, setEntity] = useState<EntityType>(entityFromUrl as EntityType);
  const [openModal, setOpenModal] = useState<boolean>(false);
  const { setTitle } = useContext(TitleContext);
  const [userHeaders, setUserHeaders] = useState<string[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const { showSnackBar } = useContext(CustomSnackBarContext);
  const [jsonData, setJsonData] = useState<{}[]>();
  const headerKeysConfig = getOwnHeadersConfig(t);
  const dispatch = useDispatch();
  const [loadingImport, setLoadingImport] = useState<boolean>(false);
  const [matches, setMatches] = useState<
    { ownHeader: OwnHeader; userHeader: string }[]
  >([]);
  const [spreadSheetsConfig, setSpreadSheetsConfig] = useState<{
    [key: string]: Matrix<CellBase<any>>[];
  }>({});
  const steps = [t('upload'), t('match_columns'), t('review'), t('done')];
  const [activeStep, setActiveStep] = useState<number>(0);
  const options: { label: string; value: EntityType }[] = [
    { label: t('work_orders'), value: 'work-orders' },
    { label: t('assets'), value: 'assets' },
    { label: t('locations'), value: 'locations' },
    { label: t('parts'), value: 'parts' },
    { label: t('meters'), value: 'meters' },
    { label: t('preventive_maintenance'), value: 'preventive-maintenances' }
  ];
  useEffect(() => {
    setTitle(t('import'));
  }, []);
  const onStartProcess = () => {
    setOpenModal(true);
  };

  const reset = () => {
    setActiveStep(0);
    setMatches([]);
    setJsonData(null);
    setSpreadSheetsConfig({});
    setUserHeaders([]);
    setLoading(false);
  };
  useEffect(() => {
    if (userHeaders.length) {
      let result: { userHeader: string; keyName: string }[] = [];
      headerKeysConfig[entity].forEach((ownHeader) => {
        const closestMatchInUserHeaders = closestMatch(
          ownHeader.label,
          userHeaders
        );
        let closestUserHeader = closestMatchInUserHeaders;
        if (Array.isArray(closestMatchInUserHeaders))
          closestUserHeader = closestMatchInUserHeaders[0];
        if (
          closestUserHeader &&
          distance(closestUserHeader as string, ownHeader.label) < 5 &&
          result.every(({ userHeader }) => userHeader !== closestUserHeader)
        ) {
          result.push({
            userHeader: userHeaders.find(
              (userHeader) => userHeader === closestUserHeader
            ),
            keyName: ownHeader.keyName
          });
        }
      });
      match(result);
    }
  }, [userHeaders]);

  const handleNext = () => {
    if (activeStep === 1) {
      if (!matches.length) {
        showSnackBar(t('match_at_least_column'), 'error');
        return;
      }
      const config = headerKeysConfig[entity];
      //check duplicates
      let duplicates = [];
      config.forEach(({ keyName, label }) => {
        let count = 0;
        matches.forEach((match) => {
          if (match.ownHeader.keyName === keyName) {
            count = count + 1;
          }
        });
        if (count > 1) {
          duplicates.push(label);
        }
      });
      if (duplicates.length) {
        showSnackBar(
          t('there_are_duplicates', { duplicates: duplicates.join(', ') }),
          'error'
        );
        return;
      }
      // check if required
      for (const header of config) {
        if (
          header.required &&
          !matches.find((match) => match.ownHeader.keyName === header.keyName)
        ) {
          showSnackBar(t('required_match', { field: header.label }), 'error');
          return;
        }
      }
    }
    setActiveStep((step) => step + 1);
  };
  const getResponseMessage = (entity: EntityType) => {
    switch (entity) {
      case 'work-orders':
        return 'import_wo_success';
      case 'assets':
        return 'import_asset_success';
      case 'locations':
        return 'import_location_success';
      case 'parts':
        return 'import_part_success';
      case 'meters':
        return 'import_meter_success';
      case 'preventive-maintenances':
        return 'import_pm_success';
    }
  };
  const downloadTemplate = () => {
    api
      .get<any>(
        `import/download-template?language=${i18n.language.toUpperCase()}&importEntity=${entity
          .toUpperCase()
          .replace('-', '_')
          .substring(0, entity.length - 1)}`,
        { raw: true }
      )
      .then((res) => res.blob())
      .then((blob) => {
        downloadFile(blob, `${entity} import template.csv`);
      });
  };
  const getMatchLabel = (userHeader: string) => {
    return matches.find(
      (headerMatching) => headerMatching.userHeader === userHeader
    )?.ownHeader?.label;
  };
  const getFormattedData = (): { value: any }[][] => {
    let result = [];
    const rowsToShow = 10;
    const data = [...jsonData].slice(0, rowsToShow);
    data.forEach((userElement) => {
      const row = headerKeysConfig[entity].map((column) => {
        const equivalent = matches.find(
          (headerMatching) =>
            headerMatching.ownHeader.keyName === column.keyName
        );
        return {
          value: equivalent ? userElement[equivalent.userHeader] : null,
          readOnly: true
        };
      });
      result.push(row);
    });
    return result;
  };
  const onImport = () => {
    setLoadingImport(true);
    const data = [...jsonData];
    const payload: ImportDTO[] = data.map((userElement) => {
      let result = {} as ImportDTO;
      headerKeysConfig[entity].forEach(({ keyName, formatter }) => {
        let value =
          userElement[
            matches.find(
              (headerMatching) => headerMatching.ownHeader.keyName === keyName
            )?.userHeader
          ];
        if (formatter) {
          value = formatter(value);
        }
        result = {
          ...result,
          [keyName]: value
        };
      });
      return result;
    });
    dispatch(importEntity(payload, entity))
      .then((response: ImportResponse) => {
        showSnackBar(
          t(getResponseMessage(entity), {
            created: response.created,
            updated: response.updated
          }),
          'success'
        );
        setOpenModal(false);
        reset();
      })
      .catch((error) => {
        showSnackBar(t('import_error'), 'error');
      })
      .finally(() => {
        setLoadingImport(false);
      });
  };
  const match = (data: { userHeader: string; keyName: string }[]) => {
    let result = [...matches];
    data.forEach(({ userHeader, keyName }) => {
      const newHeaderKey = headerKeysConfig[entity].find(
        (headerKey) => headerKey.keyName === keyName
      );
      const inheadersMatching = matches.find(
        (headerMatching) => headerMatching.userHeader === userHeader
      );
      if (inheadersMatching) {
        inheadersMatching.ownHeader = newHeaderKey;
      } else {
        result.push({ ownHeader: newHeaderKey, userHeader });
      }
    });
    setMatches(result);
  };
  const SingleHeader = ({ userHeader }: { userHeader: string }) => {
    const onChange = (event) => {
      match([{ userHeader, keyName: event.target.value }]);
    };
    const getPercent = () => {
      const rows = spreadSheetsConfig[userHeader][0];
      return (rows.filter((row) => row[0].value).length * 100) / rows.length;
    };
    return (
      <Grid item xs={12}>
        <Card sx={{ display: 'flex', flexDirection: 'row', p: 2 }}>
          <Grid container spacing={1}>
            <Grid item xs={6}>
              <Stack direction="row" spacing={1} alignItems="center">
                <Box>
                  <Typography variant={'h5'}>{userHeader}</Typography>
                </Box>
                <Select
                  value={
                    matches.find(
                      (headerMatching) =>
                        headerMatching.userHeader === userHeader
                    )?.ownHeader.keyName ?? ''
                  }
                  onChange={onChange}
                >
                  <MenuItem disabled value={''}>
                    <em>{t('select')}</em>
                  </MenuItem>
                  {headerKeysConfig[entity].map((header) => (
                    <MenuItem key={header.keyName} value={header.keyName}>
                      {header.label}
                    </MenuItem>
                  ))}
                </Select>
              </Stack>
              <Box sx={{ mt: 1 }}>
                <Spreadsheet
                  data={spreadSheetsConfig[userHeader][0].slice(0, 4)}
                />
              </Box>
            </Grid>
            <Grid item xs={6} sx={{ p: 2 }}>
              <Stack
                width={'100%'}
                height={'100%'}
                justifyContent={'center'}
                alignItems={'center'}
              >
                {!getMatchLabel(userHeader) && (
                  <WarningAmberIcon color="warning" />
                )}
                <Typography>
                  {getMatchLabel(userHeader)
                    ? t(`matched_to_field`, {
                        field: getMatchLabel(userHeader)
                      })
                    : t('no_match_yet')}
                </Typography>
                <Stack direction="row" spacing={1}>
                  <InfoTwoToneIcon />
                  <Typography>
                    {t('percent_rows_have_value', {
                      percent: getPercent().toFixed(0)
                    })}
                  </Typography>
                </Stack>
              </Stack>
            </Grid>
          </Grid>
        </Card>
      </Grid>
    );
  };
  const renderModal = () => (
    <Dialog
      fullWidth
      maxWidth="md"
      open={openModal}
      onClose={() => {
        setOpenModal(false);
        reset();
      }}
    >
      <DialogTitle
        sx={{
          p: 3
        }}
      >
        <Typography variant="h4" gutterBottom>
          {t('import')}
        </Typography>
      </DialogTitle>
      <DialogContent
        dividers
        sx={{
          p: 3
        }}
      >
        <Box>
          <Stepper activeStep={activeStep}>
            {steps.map((label, index) => (
              <Step key={index}>
                <StepLabel>{label}</StepLabel>
              </Step>
            ))}
          </Stepper>
          {activeStep == 0 ? (
            loading ? (
              <Stack
                direction="row"
                width={'100%'}
                height="100%"
                justifyContent="center"
                alignItems="center"
              >
                <CircularProgress />
              </Stack>
            ) : (
              <FileUpload
                multiple={false}
                title={t('upload')}
                type={'spreadsheet'}
                description={t('upload')}
                onDrop={(files: any) => {
                  setLoading(true);
                  var reader = new FileReader();
                  reader.onload = function (e) {
                    const data = e.target.result;
                    const file = read(data, {
                      type: 'string'
                    });
                    const sheet = file.Sheets[file.SheetNames[0]];
                    const localJsonArray: string[][] = utils.sheet_to_json(
                      sheet,
                      { header: 1 }
                    );
                    const localJson = utils.sheet_to_json(sheet);
                    setJsonData(localJson);
                    if (localJsonArray.length > 1) {
                      setUserHeaders(localJsonArray[0]);
                      const localObjectOfArrayOfArrays =
                        arrayToAoA(localJsonArray);
                      setSpreadSheetsConfig(localObjectOfArrayOfArrays);
                      setLoading(false);
                      handleNext();
                    } else {
                      showSnackBar(t('not_enough_rows'), 'error');
                    }
                    /* DO SOMETHING WITH workbook HERE */
                  };
                  reader.readAsText(files[0]);
                }}
              />
            )
          ) : activeStep === 1 ? (
            <Box>
              <Grid container spacing={1}>
                {userHeaders.map((userHeader, index) => (
                  <SingleHeader key={index} userHeader={userHeader} />
                ))}
              </Grid>
            </Box>
          ) : activeStep === 2 ? (
            <Box>
              <Spreadsheet
                data={getFormattedData()}
                columnLabels={headerKeysConfig[entity].map(
                  ({ label }) => label
                )}
              />
            </Box>
          ) : null}
        </Box>
      </DialogContent>
      <DialogActions>
        {activeStep > 0 && (
          <Button
            variant="outlined"
            onClick={() => setActiveStep((step) => step - 1)}
          >
            {t('go_back')}
          </Button>
        )}
        {!!activeStep && activeStep < steps.length - 2 && (
          <Button variant="contained" onClick={handleNext}>
            {t('next')}
          </Button>
        )}
        {activeStep === steps.length - 2 && (
          <Button
            variant="contained"
            onClick={onImport}
            disabled={loadingImport}
            startIcon={loadingImport ? <CircularProgress size="1rem" /> : null}
          >
            {t('to_import')}
          </Button>
        )}
      </DialogActions>
    </Dialog>
  );
  if (hasFeature(PlanFeature.IMPORT_CSV)) {
    if (hasViewPermission(PermissionEntity.SETTINGS))
      return (
        <>
          <Helmet>
            <title>{t('import')}</title>
          </Helmet>
          <Grid
            container
            justifyContent="center"
            alignItems="stretch"
            spacing={2}
            padding={4}
          >
            <Grid item xs={12}>
              <Card
                sx={{
                  p: 2
                }}
              >
                <Grid container spacing={2}>
                  <Grid
                    item
                    xs={12}
                    display="flex"
                    justifyContent="center"
                    alignItems="center"
                  >
                    <Typography variant="h4">{t('import_data')}</Typography>
                  </Grid>
                  <Grid
                    item
                    xs={12}
                    display="flex"
                    justifyContent="center"
                    flexDirection="column"
                    alignItems="center"
                  >
                    <Select
                      value={entity}
                      onChange={(event) => {
                        setEntity(event.target.value as EntityType);
                        window.history.replaceState(
                          null,
                          '',
                          `/app/imports/${event.target.value}`
                        );
                      }}
                    >
                      {options.map((option) => (
                        <MenuItem key={option.value} value={option.value}>
                          {option.label}
                        </MenuItem>
                      ))}
                    </Select>

                    <Button
                      sx={{ mt: 1 }}
                      variant="contained"
                      size="medium"
                      onClick={onStartProcess}
                    >
                      {t('start_import_process')}
                    </Button>
                    <Stack
                      direction={'row'}
                      sx={{ mt: 2 }}
                      onClick={downloadTemplate}
                    >
                      <DownloadTwoToneIcon color={'primary'} />
                      <Typography
                        color={'primary'}
                        style={{ cursor: 'pointer' }}
                      >
                        {t('download_template')}
                      </Typography>
                    </Stack>
                  </Grid>
                </Grid>
              </Card>
            </Grid>
          </Grid>
          {renderModal()}
        </>
      );
    else return <PermissionErrorMessage message={'no_access_page'} />;
  } else return <FeatureErrorMessage message={'no_import_access'} />;
};

export default Import;
