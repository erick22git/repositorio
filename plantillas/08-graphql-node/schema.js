const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Recurso } = require('./database');

// 1) TIPO: que campos se pueden pedir
const RecursoType = new GraphQLObjectType({
  name: 'Recurso',
  fields: {
    id:     { type: GraphQLString },
    codigo: { type: GraphQLString },
    nombre: { type: GraphQLString },
    monto:  { type: GraphQLFloat },
    estado: { type: GraphQLString }
  }
});

// 2) QUERIES: leer
const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    recursos: {
      type: new GraphQLList(RecursoType),
      resolve() {
        return Recurso.findAll();
      }
    },
    recurso: {
      type: RecursoType,
      args: { codigo: { type: new GraphQLNonNull(GraphQLString) } },
      async resolve(parent, args) {
        const r = await Recurso.findOne({ where: { codigo: args.codigo } });
        if (!r) throw new Error('No existe ' + args.codigo);
        // AQUI LOGICA
        return r;
      }
    }
  }
});

// 3) MUTATIONS: crear / cambiar
const Mutation = new GraphQLObjectType({
  name: 'Mutation',
  fields: {
    crearRecurso: {
      type: RecursoType,
      args: {
        codigo: { type: new GraphQLNonNull(GraphQLString) },
        nombre: { type: new GraphQLNonNull(GraphQLString) },
        monto:  { type: GraphQLFloat }
      },
      resolve(parent, args) {
        // AQUI LOGICA (validaciones antes de guardar)
        return Recurso.create(args);
      }
    },
    cambiarEstado: {
      type: RecursoType,
      args: {
        codigo: { type: new GraphQLNonNull(GraphQLString) },
        estado: { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, args) {
        const r = await Recurso.findOne({ where: { codigo: args.codigo } });
        if (!r) throw new Error('No existe ' + args.codigo);
        // AQUI LOGICA
        return r.update({ estado: args.estado });
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery, mutation: Mutation });
