// hooks.js
import { useState, useEffect } from "react";
function useFetch(url) {
  const [data, setData] = useState("");
  const [loading, setLoading] = useState(true);
  async function fetchUrl() {
    const response = await fetch(url);
    const text = await response.text();
    setData(text);
    setLoading(false);
  }
  useEffect(() => {
    fetchUrl();
  }, []);
  return [data, setData];
}
export { useFetch };