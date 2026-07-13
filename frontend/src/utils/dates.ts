export const dayDiff = (date1: Date, date2: Date) => {
  const diffInTime = date2.getTime() - date1.getTime();
  return Math.abs(Math.floor(diffInTime / (1000 * 3600 * 24))) - 1;
};

export const getDayAndMonth = (str: string): string => {
  const date = new Date(str).toDateString();
  const arr = date.split(' ');
  return `${arr[1]} ${arr[2]}`;
};
export const getDayAndMonthAndYear = (str: string): string => {
  const date = new Date(str).toDateString();
  const arr = date.split(' ');
  return `${arr[1]} ${arr[2]} ${arr[3]}`;
};
export const sameDay = (d1: Date, d2: Date) => {
  return (
    d1.getFullYear() === d2.getFullYear() &&
    d1.getMonth() === d2.getMonth() &&
    d1.getDate() === d2.getDate()
  );
};
