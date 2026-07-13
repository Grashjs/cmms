import type { ElementType, FC, ReactNode } from 'react';
import { format } from 'date-fns';
import PropTypes from 'prop-types';
import { Grid, IconButton, Tooltip, Typography } from '@mui/material';

import ViewAgendaTwoToneIcon from '@mui/icons-material/ViewAgendaTwoTone';

import ViewDayTwoToneIcon from '@mui/icons-material/ViewDayTwoTone';
import CalendarViewMonthTwoToneIcon from '@mui/icons-material/CalendarViewMonthTwoTone';
import ViewWeekTwoToneIcon from '@mui/icons-material/ViewWeekTwoTone';
import type { View } from 'src/models/calendar';
import { useTranslation } from 'react-i18next';
import TodayTwoToneIcon from '@mui/icons-material/TodayTwoTone';
import ArrowForwardTwoToneIcon from '@mui/icons-material/ArrowForwardTwoTone';
import ArrowBackTwoToneIcon from '@mui/icons-material/ArrowBackTwoTone';
import i18n from 'i18next';
import { supportedLanguages } from '../../../../i18n/i18n';

interface ActionsProps {
  children?: ReactNode;
  className?: string;
  date: Date;
  onNext?: () => void;
  onPrevious?: () => void;
  onToday?: () => void;
  handleCreateEvent?: () => void;
  changeView?: (view: View) => void;
  view: View;
}

interface ViewOption {
  label: string;
  value: View;
  icon: ElementType;
}

const viewOptions: ViewOption[] = [
  {
    label: 'month',
    value: 'dayGridMonth',
    icon: CalendarViewMonthTwoToneIcon
  },
  {
    label: 'week',
    value: 'timeGridWeek',
    icon: ViewWeekTwoToneIcon
  },
  {
    label: 'day',
    value: 'timeGridDay',
    icon: ViewDayTwoToneIcon
  },
  {
    label: 'agenda',
    value: 'listWeek',
    icon: ViewAgendaTwoToneIcon
  }
];

const Actions: FC<ActionsProps> = ({
  date,
  onNext,
  onPrevious,
  onToday,
  changeView,
  view
}) => {
  const { t }: { t: any } = useTranslation();
  const getLanguage = i18n.language;

  return (
    <Grid
      container
      spacing={3}
      alignItems="center"
      justifyContent="space-between"
    >
      <Grid item>
        <Tooltip arrow placement="top" title={t('previous')}>
          <IconButton color="primary" onClick={onPrevious}>
            <ArrowBackTwoToneIcon />
          </IconButton>
        </Tooltip>
        <Tooltip arrow placement="top" title={t('today')}>
          <IconButton
            color="primary"
            sx={{
              mx: 1
            }}
            onClick={onToday}
          >
            <TodayTwoToneIcon />
          </IconButton>
        </Tooltip>
        <Tooltip arrow placement="top" title={t('next')}>
          <IconButton color="primary" onClick={onNext}>
            <ArrowForwardTwoToneIcon />
          </IconButton>
        </Tooltip>
      </Grid>
      <Grid
        item
        sx={{
          display: { xs: 'none', sm: 'inline-block' }
        }}
      >
        <Typography variant="h3" color="text.primary">
          {format(date, 'MMMM yyyy', {
            locale: supportedLanguages.find(({ code }) => code === getLanguage)
              .dateLocale
          })}
        </Typography>
      </Grid>
      <Grid
        item
        sx={{
          display: { xs: 'none', sm: 'inline-block' }
        }}
      >
        {viewOptions.map((viewOption) => {
          const Icon = viewOption.icon;

          return (
            <Tooltip
              key={viewOption.value}
              arrow
              placement="top"
              title={t(viewOption.label)}
            >
              <IconButton
                color={viewOption.value === view ? 'primary' : 'secondary'}
                onClick={() => changeView(viewOption.value)}
              >
                <Icon />
              </IconButton>
            </Tooltip>
          );
        })}
      </Grid>
    </Grid>
  );
};

Actions.propTypes = {
  children: PropTypes.node,
  className: PropTypes.string,
  date: PropTypes.instanceOf(Date).isRequired,
  onNext: PropTypes.func,
  onPrevious: PropTypes.func,
  onToday: PropTypes.func,
  handleCreateEvent: PropTypes.func,
  changeView: PropTypes.func,
  view: PropTypes.oneOf([
    'dayGridMonth',
    'timeGridWeek',
    'timeGridDay',
    'listWeek'
  ])
};

Actions.defaultProps = {
  onNext: () => {},
  onPrevious: () => {},
  onToday: () => {},
  handleCreateEvent: () => {},
  changeView: () => {}
};

export default Actions;
