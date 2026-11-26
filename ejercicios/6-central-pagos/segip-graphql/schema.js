const { GraphQLObjectType, GraphQLString, GraphQLSchema, GraphQLNonNull } = require('graphql');

// SIN base de datos: los datos viven en un arreglo
const personas = [
  { ci: '11021654', nombre: 'Juan Perez' },
  { ci: '5551234',  nombre: 'Ana Rojas' }
];

const PersonaType = new GraphQLObjectType({
  name: 'Persona',
  fields: {
    ci:     { type: GraphQLString },
    nombre: { type: GraphQLString }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    persona: {
      type: PersonaType,
      args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
      resolve: (parent, { ci }) => personas.find(p => p.ci === ci) || null
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery });
