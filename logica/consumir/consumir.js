// Funciona en el navegador y en Node 18 o superior

// REST
async function rest() {
  const r = await fetch('http://localhost:8000/api/recursos', {
    headers: { Accept: 'application/json' }       // , Authorization: 'Bearer ' + token
  });
  console.log(r.status, await r.json());
}

// GraphQL
async function graphql() {
  const r = await fetch('http://localhost:4000/graphql', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      query: 'query($c: String!) { recurso(codigo: $c) { nombre estado } }',
      variables: { c: 'A1' }
    })
  });
  const { data, errors } = await r.json();        // GraphQL: mirar errors
  console.log(errors ? errors[0].message : data);
}

// SOAP
async function soap() {
  const sobre = `<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"><soap:Body>
    <Verificar xmlns="http://tempuri.org/"><codigo>A1</codigo></Verificar>
  </soap:Body></soap:Envelope>`;
  const r = await fetch('http://localhost:51305/Servicio.asmx', {
    method: 'POST',
    headers: { 'Content-Type': 'text/xml; charset=utf-8', SOAPAction: '"http://tempuri.org/Verificar"' },
    body: sobre
  });
  console.log(await r.text());
}

rest().catch(e => console.log('REST no responde:', e.message));
graphql().catch(e => console.log('GraphQL no responde:', e.message));
soap().catch(e => console.log('SOAP no responde:', e.message));
