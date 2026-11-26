const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Candidato, Mesa, Acta, Voto } = require('./database');

const CandidatoResultadoType = new GraphQLObjectType({
  name: 'CandidatoResultado',
  fields: {
    id:         { type: GraphQLInt },
    nombre:     { type: GraphQLString },
    votos:      { type: GraphQLInt },
    porcentaje: { type: GraphQLFloat }
  }
});

const ResultadoType = new GraphQLObjectType({
  name: 'Resultado',
  fields: {
    cargo:           { type: GraphQLString },
    mesasTotal:      { type: GraphQLInt },
    mesasComputadas: { type: GraphQLInt },
    avance:          { type: GraphQLFloat },
    validos:         { type: GraphQLInt },
    blancos:         { type: GraphQLInt },
    nulos:           { type: GraphQLInt },
    emitidos:        { type: GraphQLInt },
    candidatos:      { type: new GraphQLList(CandidatoResultadoType) },
    situacion:       { type: GraphQLString }
  }
});

const MesaType = new GraphQLObjectType({
  name: 'Mesa',
  fields: {
    numero:      { type: GraphQLInt },
    recinto:     { type: GraphQLString },
    habilitados: { type: GraphQLInt },
    computada:   { type: GraphQLString },
    candidatos:  { type: new GraphQLList(CandidatoResultadoType) },
    blancos:     { type: GraphQLInt },
    nulos:       { type: GraphQLInt },
    participacion: { type: GraphQLFloat }
  }
});

function redondear(n) {
  return Math.round(n * 100) / 100;
}

async function calcular(cargo, actas) {
  const candidatos = await Candidato.findAll({ where: { cargo } });
  const conteo = {};
  candidatos.forEach(c => { conteo[c.id] = 0; });
  let blancos = 0, nulos = 0;
  for (const a of actas) {
    blancos += a.blancos;
    nulos += a.nulos;
    for (const v of a.Votos) conteo[v.candidato_id] = (conteo[v.candidato_id] || 0) + v.cantidad;
  }
  const validos = Object.values(conteo).reduce((s, x) => s + x, 0);
  const lista = candidatos.map(c => ({
    id: c.id,
    nombre: c.nombres + ' ' + c.apellidos,
    votos: conteo[c.id],
    porcentaje: validos === 0 ? 0 : redondear(conteo[c.id] * 100 / validos)
  })).sort((x, y) => y.votos - x.votos);
  return { lista, validos, blancos, nulos };
}

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    resultados: {
      type: ResultadoType,
      args: { cargo: { type: new GraphQLNonNull(GraphQLString) } },
      async resolve(parent, args) {
        const mesasTotal = await Mesa.count();
        const actas = await Acta.findAll({ where: { cargo: args.cargo }, include: [Voto] });
        const { lista, validos, blancos, nulos } = await calcular(args.cargo, actas);
        if (lista.length === 0) throw new Error('No hay candidatos para el cargo ' + args.cargo);

        let situacion;
        if (validos === 0) {
          situacion = 'Sin votos computados';
        } else if (lista[0].porcentaje > 50) {
          situacion = 'Gana ' + lista[0].nombre + ' con ' + lista[0].porcentaje + '%';
        } else if (lista.length > 1 && lista[0].votos === lista[1].votos) {
          situacion = 'Empate entre ' + lista[0].nombre + ' y ' + lista[1].nombre;
        } else {
          situacion = 'Segunda vuelta entre ' + lista[0].nombre + ' y ' + lista[1].nombre;
        }

        return {
          cargo: args.cargo,
          mesasTotal,
          mesasComputadas: actas.length,
          avance: mesasTotal === 0 ? 0 : redondear(actas.length * 100 / mesasTotal),
          validos, blancos, nulos,
          emitidos: validos + blancos + nulos,
          candidatos: lista,
          situacion
        };
      }
    },
    mesa: {
      type: MesaType,
      args: { numero: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const m = await Mesa.findOne({ where: { numero: args.numero } });
        if (!m) throw new Error('No existe la mesa ' + args.numero);
        const acta = await Acta.findOne({ where: { mesa_id: m.id }, include: [Voto] });
        if (!acta) {
          return { numero: m.numero, recinto: m.recinto, habilitados: m.habilitados, computada: 'no', candidatos: [], blancos: 0, nulos: 0, participacion: 0 };
        }
        const { lista, validos, blancos, nulos } = await calcular(acta.cargo, [acta]);
        return {
          numero: m.numero,
          recinto: m.recinto,
          habilitados: m.habilitados,
          computada: 'si',
          candidatos: lista,
          blancos, nulos,
          participacion: redondear((validos + blancos + nulos) * 100 / m.habilitados)
        };
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery });
