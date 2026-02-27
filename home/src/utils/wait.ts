const wait = (time: number): Promise<void> =>
  new Promise((res) => setTimeout(res, time));

export default wait;
