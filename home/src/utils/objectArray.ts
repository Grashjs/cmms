const objectArray = (arr: any[], key: string = 'id') =>
  arr.reduce((acc, cur) => {
    acc[cur[key]] = cur;
    return acc;
  }, {});

export default objectArray;
