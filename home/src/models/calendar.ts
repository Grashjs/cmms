export interface Event {
  id: string;
  allDay: boolean;
  color?: string;
  description: string;
  end: Date;
  start: Date;
  title: string;
}

export type View = 'dayGridMonth' | 'timeGridWeek' | 'timeGridDay' | 'listWeek';
