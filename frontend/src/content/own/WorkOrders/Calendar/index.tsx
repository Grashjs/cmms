import { useEffect, useRef, useState } from 'react';
import frLocale from '@fullcalendar/core/locales/fr';
import enLocale from '@fullcalendar/core/locales/en-gb';
import FullCalendar from '@fullcalendar/react';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import listPlugin from '@fullcalendar/list';
import {
  Box,
  Card,
  CircularProgress,
  Divider,
  Grid,
  Stack,
  styled,
  useMediaQuery,
  useTheme
} from '@mui/material';

import type { View } from 'src/models/calendar';
import { useDispatch, useSelector } from 'src/store';
import { selectEvent } from 'src/slices/calendar';
import WorkOrder, { Priority } from 'src/models/owns/workOrder';
import { CalendarEvent, getWorkOrderEvents } from 'src/slices/workOrder';
import Actions from './Actions';
import i18n from 'i18next';
import PreventiveMaintenance from 'src/models/owns/preventiveMaintenance';
import { usePrevious } from '../../../../hooks/usePrevious';
import { supportedLanguages } from '../../../../i18n/i18n';

const FullCalendarWrapper = styled(Box)(
  ({ theme }) => `
    padding: ${theme.spacing(3)};
    position: relative;
   
    & .fc-license-message {
      display: none;
    }
    .fc {
      .fc-daygrid-day,.fc-timegrid-slot{
        cursor: pointer;
      }
      .fc-col-header-cell {
        padding: ${theme.spacing(1)};
        background: ${theme.colors.alpha.black[5]};
      }

      .fc-scrollgrid {
        border: 2px solid ${theme.colors.alpha.black[10]};
        border-right-width: 1px;
        border-bottom-width: 1px;
      }

      .fc-cell-shaded,
      .fc-list-day-cushion {
        background: ${theme.colors.alpha.black[5]};
      }

      .fc-list-event-graphic {
        padding-right: ${theme.spacing(1)};
      }

      .fc-theme-standard td, .fc-theme-standard th,
      .fc-col-header-cell {
        border: 1px solid ${theme.colors.alpha.black[10]};
      }

      .fc-event {
        padding: ${theme.spacing(0.1)} ${theme.spacing(0.3)};
      }

      .fc-list-day-side-text {
        font-weight: normal;
        color: ${theme.colors.alpha.black[70]};
      }

      .fc-list-event:hover td,
      td.fc-daygrid-day.fc-day-today {
        background-color: ${theme.colors.primary.lighter};
      }

      td.fc-daygrid-day:hover,
      .fc-highlight {
        background: ${theme.colors.alpha.black[10]};
      }

      .fc-daygrid-dot-event:hover, 
      .fc-daygrid-dot-event.fc-event-mirror {
        background: ${theme.colors.primary.lighter};
      }

      .fc-daygrid-day-number {
        padding: ${theme.spacing(1)};
        font-weight: bold;
      }

      .fc-list-sticky .fc-list-day > * {
        background: ${theme.colors.alpha.black[5]} !important;
      }

      .fc-cell-shaded, 
      .fc-list-day-cushion {
        background: ${theme.colors.alpha.black[10]} !important;
        color: ${theme.colors.alpha.black[70]} !important;
      }

      &.fc-theme-standard td, 
      &.fc-theme-standard th,
      &.fc-theme-standard .fc-list {
        border-color: ${theme.colors.alpha.black[30]};
      }
    }
`
);

interface Event {
  id: string;
  allDay: boolean;
  color?: string;
  description: string;
  end: Date;
  start: Date;
  title: string;
  extendedProps: { type: string };
}

interface OwnProps {
  handleAddWorkOrder: (date: Date) => void;
  handleOpenDetails: (id: number, type: string) => void;
}

function ApplicationsCalendar({
  handleAddWorkOrder,
  handleOpenDetails
}: OwnProps) {
  const theme = useTheme();

  const calendarRef = useRef<FullCalendar | null>(null);
  const mobile = useMediaQuery(theme.breakpoints.down('md'));
  const dispatch = useDispatch();
  const { calendar, loadingGet } = useSelector((state) => state.workOrders);
  const [date, setDate] = useState<Date>(new Date());
  const [view, setView] = useState<View>('timeGridWeek');
  const getLanguage = i18n.language;
  const viewsOrder: View[] = [
    'dayGridMonth',
    'timeGridWeek',
    'listWeek',
    'timeGridDay'
  ];
  const previousView = usePrevious(view);
  const getColor = (priority: Priority) => {
    switch (priority) {
      case 'HIGH':
        return theme.colors.error.main;
      case 'MEDIUM':
        return theme.colors.warning.main;
      case 'LOW':
        return theme.colors.info.main;
      case 'NONE':
        return theme.colors.primary.main;
      default:
        break;
    }
  };
  const getEventFromWO = (
    eventPayload: CalendarEvent<WorkOrder | PreventiveMaintenance>
  ): Event => {
    return {
      id: eventPayload.event.id.toString(),
      allDay: false,
      color: getColor(eventPayload.event.priority),
      description: eventPayload.event?.description,
      end: new Date(eventPayload.date),
      start: new Date(eventPayload.date),
      title: eventPayload.event.title,
      extendedProps: { type: eventPayload.type }
    };
  };
  const handleDateToday = (): void => {
    const calItem = calendarRef.current;

    if (calItem) {
      const calApi = calItem.getApi();

      calApi.today();
      setDate(calApi.getDate());
    }
  };
  useEffect(() => {
    const calItem = calendarRef.current;
    const newView = calItem.getApi().view;
    if (
      previousView &&
      previousView !== view &&
      viewsOrder.findIndex((v) => v === previousView) <
        viewsOrder.findIndex((v) => v === view)
    ) {
      return;
    }
    const start = newView.activeStart;
    const end = newView.activeEnd;
    dispatch(getWorkOrderEvents(start, end));
  }, [date, view]);
  const changeView = (changedView: View): void => {
    const calItem = calendarRef.current;

    if (calItem) {
      const calApi = calItem.getApi();

      calApi.changeView(changedView);
      setView(changedView);
    }
  };

  const handleDatePrev = (): void => {
    const calItem = calendarRef.current;

    if (calItem) {
      const calApi = calItem.getApi();

      calApi.prev();
      setDate(calApi.getDate());
    }
  };

  const handleDateNext = (): void => {
    const calItem = calendarRef.current;

    if (calItem) {
      const calApi = calItem.getApi();

      calApi.next();
      setDate(calApi.getDate());
    }
  };

  const handleEventSelect = (arg: any): void => {
    dispatch(selectEvent(arg.event.id));
  };

  return (
    <Grid item xs={12}>
      <Card>
        <Box p={3}>
          <Actions
            date={date}
            onNext={handleDateNext}
            onPrevious={handleDatePrev}
            onToday={handleDateToday}
            changeView={changeView}
            view={view}
          />
        </Box>
        <Divider />
        <FullCalendarWrapper>
          {loadingGet && (
            <Stack position="absolute" top={'45%'} left={'45%'} zIndex={10}>
              <CircularProgress size={64} />
            </Stack>
          )}
          <FullCalendar
            allDayMaintainDuration
            initialDate={date}
            initialView={view}
            locale={
              supportedLanguages.find(({ code }) => code === getLanguage)
                .calendarLocale
            }
            droppable
            eventDisplay="block"
            eventClick={(arg) =>
              handleOpenDetails(
                Number(arg.event.id),
                arg.event.extendedProps.type
              )
            }
            dateClick={(event) => handleAddWorkOrder(event.date)}
            dayMaxEventRows={4}
            events={calendar.events.map((eventPayload) =>
              getEventFromWO(eventPayload)
            )}
            headerToolbar={false}
            height={660}
            ref={calendarRef}
            rerenderDelay={10}
            weekends
            plugins={[
              dayGridPlugin,
              timeGridPlugin,
              interactionPlugin,
              listPlugin
            ]}
          />
        </FullCalendarWrapper>
      </Card>
    </Grid>
  );
}

export default ApplicationsCalendar;
