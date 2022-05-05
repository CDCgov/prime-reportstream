import { useEffect, useState } from 'react';
import ReactMarkdown from 'react-markdown'

import mdPath from "../../../content/programmers-guide.md";
import site from "../../../content/site.json";



export const ProgrammersGuide = () => {
  const [markdownContent, setMarkdownContent] = useState('');

  // Approach from: https://stackoverflow.com/questions/65395125/how-to-load-an-md-file-on-build-when-using-create-react-app-and-typescript
  // TODO(mreifman): Explore whether it makes sense to eject from create-react-app and modify the
  // webpack config
  useEffect(() => {
    fetch(mdPath)
      .then((response) => response.text())
      .then((text) => {
        setMarkdownContent(text);
      });
  }, []);


  // console.log(md)
  return (<>
    <ReactMarkdown>{markdownContent}</ReactMarkdown>
    <pre>{JSON.stringify(site, null, 2)}</pre>
  </>)
}
