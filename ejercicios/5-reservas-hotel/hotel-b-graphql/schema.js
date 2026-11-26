const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Habitacion } = require('./database');

const HabitacionType = new GraphQLObjectType({
  name: 'Habitacion',
  fields: {
    codHabitacion: { type: GraphQLString },
    tipo:          { type: GraphQLString },
    capacidad:     { type: GraphQLInt },
    tarifa:        { type: GraphQLFloat },
    disponible:    { type: GraphQLBoolean }
  }
});

async function buscar(codHabitacion) {
  const h = await Habitacion.findOne({ where: { codHabitacion } });
  if (!h) throw new Error('No existe la habitacion ' + codHabitacion);
  return h;
}

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    habitaciones: {
      type: new GraphQLList(HabitacionType),
      resolve: () => Habitacion.findAll()
    },
    habitacion: {
      type: HabitacionType,
      args: { codHabitacion: { type: new GraphQLNonNull(GraphQLString) } },
      resolve: (parent, args) => buscar(args.codHabitacion)
    }
  }
});

const Mutation = new GraphQLObjectType({
  name: 'Mutation',
  fields: {
    actualizarDisponibilidad: {
      type: HabitacionType,
      args: {
        codHabitacion: { type: new GraphQLNonNull(GraphQLString) },
        disponible:    { type: new GraphQLNonNull(GraphQLBoolean) }
      },
      async resolve(parent, args) {
        const h = await buscar(args.codHabitacion);
        return h.update({ disponible: args.disponible });
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery, mutation: Mutation });
