
class ModelObject < BasicObject
  @@ids = 0
  @@SchemaPointer = nil
  @@KlassPointer = nil
  @@FieldPointer = nil
  @@PrimitiveTypePointer = nil

  def initialize()
    @fields = {}
    @id = @@ids += 1
  end

  def [](s)
    @fields[s.to_sym]
  end

  def method_missing(name, *args, &block)
    if (name.to_s =~ /^([a-zA-Z0-9\_]*)=$/)
      @fields[$1.to_sym] = args[0]
    else
      @fields[name]
    end
  end

  def to_s
    "model(#{_id})"
  end

  def _id
    return @id
  end
  
  def self.setup(a, b, c, d)
      @@SchemaPointer = a.klass
	  @@KlassPointer = b.klass
	  @@FieldPointer = c.klass
	  @@PrimitiveTypePointer = d.klass
	end
end

class SchemaObject < ModelObject
	def metaclass
		@@SchemaPointer
	end
end
class KlassObject < ModelObject
	def metaclass
		@@KlassPointer
	end
end
class FieldObject < ModelObject
	def metaclass
		@@FieldPointer
	end
end
class PrimitiveTypeObject < ModelObject
	def metaclass
		@@PrimitiveTypePointer
	end
end

class SchemaGenerator
  ## NB: to use this schemagenerator, be careful with names of classes
  ## defined using klass(): if you use a name that collides with any name
  ## included in Kernel, it'll break. 
  ## Todo: fix this?

  class Wrap < BasicObject
    attr_reader :klass
    def initialize(m, builder)
      @klass = m
      @builder = builder
    end

    def method_missing(name)
      @builder.get_field(@klass, name.to_s)
    end
  end


  @@schema = nil
  @@classes = {}
  @@primitives = {}
  @@current = nil

  def self.schema
    @@schema ||= SchemaObject.new
    @@schema.name = self.to_s
    @@schema.classes = @@classes.values
    @@schema.primitives = @@primitives.values
    return @@schema
  end
    

  class << self
    def primitive(name)
      m = PrimitiveTypeObject.new
      m.name = name.to_s
      @@primitives[name] = m
    end
      
    def klass(wrapped, opts = {}, &block)
      m = wrapped.klass
      m.super = opts[:super] ? opts[:super].klass : nil
      m.super.subtypes << m if m.super
      @@current = m
      yield
    end

    def field(name, opts = {})
      f = get_field(@@current, name.to_s)
      t = opts[:type]
      f.type = @@primitives[t] || t.klass
      f.optional = opts[:optional] || false
      f.many = opts[:many] || false
      f.inverse = opts[:inverse]
      f.inverse.inverse = f if f.inverse
    end


    def const_missing(name)
      Wrap.new(get_class(name.to_s), self)
    end

    def get_field(klass, name)
      klass.fields.each do |f|
        return f if f.name == name
      end
      f = FieldObject.new
      #puts "Creating field #{name} (#{f._id})"
      f.name = name
      klass.fields << f
      f.owner = klass
      return f
    end

    def get_class(name)
      @@classes[name] ||= KlassObject.new
      m = @@classes[name]
      #puts "Getting class #{name} (#{m._id})"
      m.name = name
      m.fields ||= []
      m.subtypes ||= []
      return m
    end

  end

end

class SchemaSchema < SchemaGenerator
  primitive :str
  primitive :int
  primitive :bool

  klass Schema do
    field :name, :type => :str
    field :classes, :type => Klass, :optional => true, :many => true, :inverse => Klass.schema
    field :primitives, :type => Primitive, :optional => true, :many => true
  end
    
  klass Type do
  end

  klass Primitive, :super => Type do
    field :name, :type => :str
  end

  klass Klass, :super => Type do
    field :name, :type => :str
    field :super, :type => Klass, :optional => true, :inverse => Klass.subtypes
    field :subtypes, :type => Klass, :optional => true, :many => true
    field :fields, :type => Field, :optional => true, :many => true, :inverse => Field.owner
    field :schema, :type => Schema
  end

  klass Field do
    field :owner, :type => Klass, :inverse => Klass.fields
    field :name, :type => :str
    field :type, :type => Type
    field :optional, :type => :bool
    field :many, :type => :bool
    field :inverse, :type => Field, :optional => true, :inverse => Field.inverse
  end

  ModelObject.setup(Schema, Klass, Field, Primitive);

end
