const {
  GraphQLObjectType, GraphQLString, GraphQLFloat, GraphQLInt, GraphQLBoolean,
  GraphQLSchema, GraphQLList, GraphQLNonNull
} = require('graphql');
const { Cita } = require('./database');
const { buscarPaciente } = require('./laravel');

const ESPECIALIDADES = { 'Medicina General': 50, 'Pediatria': 80, 'Cardiologia': 150, 'Traumatologia': 120 };
const HORAS = ['08:00', '08:30', '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
               '14:00', '14:30', '15:00', '15:30', '16:00', '16:30', '17:00', '17:30'];

const PacienteType = new GraphQLObjectType({
  name: 'Paciente',
  fields: {
    ci:        { type: GraphQLString },
    nombres:   { type: GraphQLString },
    apellidos: { type: GraphQLString },
    telefono:  { type: GraphQLString },
    seguro:    { type: GraphQLBoolean }
  }
});

const CitaType = new GraphQLObjectType({
  name: 'Cita',
  fields: {
    id:           { type: GraphQLInt },
    ci_paciente:  { type: GraphQLString },
    paciente:     { type: GraphQLString },
    especialidad: { type: GraphQLString },
    fecha:        { type: GraphQLString },
    hora:         { type: GraphQLString },
    costo:        { type: GraphQLFloat },
    estado:       { type: GraphQLString }
  }
});

const RootQuery = new GraphQLObjectType({
  name: 'RootQueryType',
  fields: {
    citas: {
      type: new GraphQLList(CitaType),
      args: { ci: { type: GraphQLString }, fecha: { type: GraphQLString }, estado: { type: GraphQLString } },
      resolve(parent, args) {
        const where = {};
        if (args.ci) where.ci_paciente = args.ci;
        if (args.fecha) where.fecha = args.fecha;
        if (args.estado) where.estado = args.estado;
        return Cita.findAll({ where, order: [['fecha', 'ASC'], ['hora', 'ASC']] });
      }
    },
    cita: {
      type: CitaType,
      args: { id: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const c = await Cita.findByPk(args.id);
        if (!c) throw new Error('No existe la cita ' + args.id);
        return c;
      }
    },
    paciente: {
      type: PacienteType,
      args: { ci: { type: new GraphQLNonNull(GraphQLString) } },
      async resolve(parent, args) {
        const p = await buscarPaciente(args.ci);
        if (!p) throw new Error('El paciente ' + args.ci + ' no esta registrado');
        return p;
      }
    },
    horariosLibres: {
      type: new GraphQLList(GraphQLString),
      args: {
        especialidad: { type: new GraphQLNonNull(GraphQLString) },
        fecha:        { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, args) {
        const ocupadas = await Cita.findAll({
          where: { especialidad: args.especialidad, fecha: args.fecha, estado: 'reservada' }
        });
        const tomadas = ocupadas.map(c => c.hora);
        return HORAS.filter(h => !tomadas.includes(h));
      }
    }
  }
});

const Mutation = new GraphQLObjectType({
  name: 'Mutation',
  fields: {
    reservarCita: {
      type: CitaType,
      args: {
        ci:           { type: new GraphQLNonNull(GraphQLString) },
        especialidad: { type: new GraphQLNonNull(GraphQLString) },
        fecha:        { type: new GraphQLNonNull(GraphQLString) },
        hora:         { type: new GraphQLNonNull(GraphQLString) }
      },
      async resolve(parent, args) {
        if (!(args.especialidad in ESPECIALIDADES)) {
          throw new Error('Especialidad no valida. Opciones: ' + Object.keys(ESPECIALIDADES).join(', '));
        }
        if (!/^\d{4}-\d{2}-\d{2}$/.test(args.fecha) || isNaN(Date.parse(args.fecha))) {
          throw new Error('La fecha debe tener formato AAAA-MM-DD');
        }
        if (!HORAS.includes(args.hora)) {
          throw new Error('Hora no valida. Atencion cada 30 minutos de 08:00 a 11:30 y de 14:00 a 17:30');
        }

        const paciente = await buscarPaciente(args.ci);
        if (!paciente) throw new Error('El paciente ' + args.ci + ' no esta registrado');

        const ocupado = await Cita.findOne({
          where: { especialidad: args.especialidad, fecha: args.fecha, hora: args.hora, estado: 'reservada' }
        });
        if (ocupado) throw new Error('Ese horario ya esta ocupado');

        const mismoDia = await Cita.findOne({
          where: { ci_paciente: args.ci, fecha: args.fecha, especialidad: args.especialidad, estado: 'reservada' }
        });
        if (mismoDia) throw new Error('El paciente ya tiene una cita de ' + args.especialidad + ' ese dia');

        return Cita.create({
          ci_paciente: args.ci,
          paciente: paciente.nombres + ' ' + paciente.apellidos,
          especialidad: args.especialidad,
          fecha: args.fecha,
          hora: args.hora,
          costo: paciente.seguro ? 0 : ESPECIALIDADES[args.especialidad]
        });
      }
    },
    cancelarCita: {
      type: CitaType,
      args: { id: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const c = await Cita.findByPk(args.id);
        if (!c) throw new Error('No existe la cita ' + args.id);
        if (c.estado !== 'reservada') throw new Error('La cita ya fue ' + c.estado);
        c.estado = 'cancelada';
        await c.save();
        return c;
      }
    },
    atenderCita: {
      type: CitaType,
      args: { id: { type: new GraphQLNonNull(GraphQLInt) } },
      async resolve(parent, args) {
        const c = await Cita.findByPk(args.id);
        if (!c) throw new Error('No existe la cita ' + args.id);
        if (c.estado !== 'reservada') throw new Error('La cita ya fue ' + c.estado);
        c.estado = 'atendida';
        await c.save();
        return c;
      }
    }
  }
});

module.exports = new GraphQLSchema({ query: RootQuery, mutation: Mutation });
