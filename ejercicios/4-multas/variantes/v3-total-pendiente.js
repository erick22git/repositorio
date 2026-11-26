// En schema.js, dentro de "fields" de RootQuery, agregar (despues de verificarMulta, con una coma):
totalPendiente: {
  type: GraphQLFloat,
  args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
  async resolve(parent, { ci }) {
    const total = await Multa.sum('monto', { where: { ci, estado: 'pendiente' } });
    return total || 0;
  }
},
