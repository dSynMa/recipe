import React, { useEffect, useRef } from 'react';

function Graph(props){
    const svg = useRef(null);
    useEffect(()=>{
        if(svg.current){
            svg.current.appendChild(props.svg)
        } 
    }, []);

    return (
        <div style={{float: "left"}} ref={svg}/>
    );
}

export default Graph;