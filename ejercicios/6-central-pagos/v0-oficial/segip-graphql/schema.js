const { GraphQLObjectType, GraphQLString, GraphQLSchema, GraphQLList, GraphQLNonNull } = require('graphql');

// SEGIP: recurso persona con los campos del enunciado. Datos en memoria.
const personas = [
  { ci: '11021654', nombres: 'Juan', primerApellido: 'Perez', segundoApellido: 'Segovia' },
  { ci: '5551234',  nombres: 'Ana',  primerApellido: 'Rojas', segundoApellido: 'Flores' },
  { ci: '7778899',  nombres: 'Luis', primerApellido: 'Vargas', segundoApellido: '' }
];

const PersonaType = new GraphQLObjectType({
  name: 'Persona',
  fields: {
    ci:              { type: GraphQLString },
    nombres:         { type: GraphQLString },
    primerApellido:  { type: GraphQLString },
    segundoApellido: { type: GraphQLString }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    // permitir buscar por CI (lo pide el enunciado)
    persona: {
      type: PersonaType,
      args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
      resolve: (parent, { ci }) => personas.find(p => p.ci === ci.trim()) || null
    },
    personas: {
      type: new GraphQLList(PersonaType),
      resolve: () => personas
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery });
