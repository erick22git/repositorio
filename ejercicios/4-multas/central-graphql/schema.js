const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Multa } = require('./database');

const MultaType = new GraphQLObjectType({
  name: 'Multa',
  fields: {
    codigo: { type: GraphQLString },
    ci:     { type: GraphQLString },
    nombre: { type: GraphQLString },
    fecha:  { type: GraphQLString },
    tipo:   { type: GraphQLString },
    lugar:  { type: GraphQLString },
    monto:  { type: GraphQLFloat },
    estado: { type: GraphQLString }
  }
});

// Respuesta de la verificacion que pide la Entidad Recaudadora
const VerificacionType = new GraphQLObjectType({
  name: 'Verificacion',
  fields: {
    sePuedePagar: { type: GraphQLBoolean },
    mensaje:      { type: GraphQLString },
    multa:        { type: MultaType }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    multas: {
      type: new GraphQLList(MultaType),
      args: { ci: { type: GraphQLString } },           // opcional
      resolve(parent, args) {
        return Multa.findAll(args.ci ? { where: { ci: args.ci } } : {});
      }
    },
    verificarMulta: {
      type: VerificacionType,
      args: {
        codigo: { type: new GraphQLNonNull(GraphQLString) },
        ci:     { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, { codigo, ci }) {
        const multa = await Multa.findOne({ where: { codigo } });
        // 1) existe  2) es de ese conductor  3) no esta pagada ni anulada
        if (!multa) return { sePuedePagar: false, mensaje: 'La multa no existe' };
        if (multa.ci !== ci) return { sePuedePagar: false, mensaje: 'No fue emitida a ese conductor' };
        if (multa.estado !== 'pendiente') {
          return { sePuedePagar: false, mensaje: 'La multa esta ' + multa.estado, multa };
        }
        return { sePuedePagar: true, mensaje: 'Pendiente de pago', multa };
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery });
