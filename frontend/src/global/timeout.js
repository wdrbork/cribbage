function timeout(ms) {
  return new Promise((res) => setTimeout(res, ms));
}

export default timeout;
