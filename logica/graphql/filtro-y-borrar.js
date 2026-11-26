// Pegar dentro de "fields" de RootQuery en schema.js
// (arriba: const { Op } = require('sequelize');)
recursosPorEstado: {
  type: new GraphQLList(RecursoType),
  args: { estado: { type: GraphQLString }, min: { type: GraphQLFloat } },
  resolve(parent, { estado, min }) {
    const where = {};
    if (estado) where.estado = estado;
    if (min) where.monto = { [Op.gte]: min };
    return Recurso.findAll({ where });
  }
},

// Pegar dentro de "fields" de Mutation
eliminarRecurso: {
  type: GraphQLBoolean,
  args: { codigo: { type: new GraphQLNonNull(GraphQLString) } },
  async resolve(parent, { codigo }) {
    const borrados = await Recurso.destroy({ where: { codigo } });
    if (!borrados) throw new Error('No existe ' + codigo);
    return true;
  }
},
