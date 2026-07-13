import { Card, Grid, Stack, Typography, useTheme } from '@mui/material';
import CheckTwoToneIcon from '@mui/icons-material/CheckTwoTone';
import CloseTwoToneIcon from '@mui/icons-material/CloseTwoTone';
import { splitToChunks } from '../../../../utils/array';
import { PlanFeature } from '../../../../models/owns/subscriptionPlan';
import { useTranslation } from 'react-i18next';

interface PlanFeatureProps {
  features: PlanFeature[];
}

function PlanFeatures(props: PlanFeatureProps) {
  const { features } = props;
  const { t }: { t: any } = useTranslation();
  const theme = useTheme();
  const basicFeatures = [
    'WORK_ORDER',
    'REQUEST',
    'PART',
    'MOBILE_APP',
    'WORK_ORDER_HISTORY'
  ].map((feature) => ({
    name: feature,
    isBasic: true
  }));
  const completeFeatures = [
    ...basicFeatures,
    ...Object.values(PlanFeature).map((feature) => ({
      name: feature,
      isBasic: false
    }))
  ];
  return (
    <Card
      sx={{
        background: `${theme.colors.gradients.blue4}`,
        color: `${theme.palette.getContrastText(theme.colors.primary.main)}`,
        display: 'flex',
        alignItems: 'flex-start',
        px: 3,
        py: 5,
        mb: 2
      }}
    >
      <Grid container>
        {splitToChunks(completeFeatures, 3).map((featuresColumn, index) => (
          <Grid key={index} item xs={12} md={4}>
            {featuresColumn.map((feature, index) => (
              <Stack
                key={index}
                spacing={1}
                direction="row"
                alignItems="center"
              >
                {features.includes(feature.name) || feature.isBasic ? (
                  <CheckTwoToneIcon />
                ) : (
                  <CloseTwoToneIcon color="error" />
                )}
                <Typography>{t(`${feature.name}_feature`)}</Typography>
              </Stack>
            ))}
          </Grid>
        ))}
      </Grid>
    </Card>
  );
}

export default PlanFeatures;
