const API = 'http://127.0.0.1:8000/api';
const USUARIO = { email: 'admin@test.com', password: '123456' };

let token = '';

async function login() {
  const r = await fetch(API + '/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(USUARIO)
  });
  if (!r.ok) throw new Error('No pude iniciar sesion en la API de pacientes');
  token = (await r.json()).token;
}

async function pedir(url) {
  if (!token) await login();
  let r = await fetch(url, { headers: { Accept: 'application/json', Authorization: 'Bearer ' + token } });
  if (r.status === 401) {
    await login();
    r = await fetch(url, { headers: { Accept: 'application/json', Authorization: 'Bearer ' + token } });
  }
  return r;
}

async function buscarPaciente(ci) {
  let r;
  try {
    r = await pedir(API + '/pacientes/' + encodeURIComponent(ci));
  } catch (e) {
    throw new Error('La API de pacientes (Laravel) no responde');
  }
  if (r.status === 404) return null;
  if (!r.ok) throw new Error('Error ' + r.status + ' al consultar el paciente');
  return r.json();
}

module.exports = { buscarPaciente };
