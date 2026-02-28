export const enumerate = (array: string[]) =>
  array.reduce(
    (acc, value, index) => acc + `${index !== 0 ? ',' : ''} ${value}`,
    ''
  );
