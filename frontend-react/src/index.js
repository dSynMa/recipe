import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import EditorTab from './EditorTab';
import reportWebVitals from './reportWebVitals';
import 'bootstrap/dist/css/bootstrap.min.css';

const root = document.getElementById('root');

function openTab() {
  ReactDOM.render(<React.StrictMode><EditorTab /></React.StrictMode>, root);
}

openTab();
// Release the editor on navigation and acquire it again on back/forward cache restoration.
window.addEventListener('pagehide', () => ReactDOM.unmountComponentAtNode(root));
window.addEventListener('pageshow', event => {
  if (event.persisted) openTab();
});

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
