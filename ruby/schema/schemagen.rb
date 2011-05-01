
class SchemaModel #< BasicObject
  @@ids = 0

  attr_accessor :schema_class
	
  def initialize()
    @fields = {}
    @id = @@ids += 1
  end

  def [](k)
    raise "undefined internal field #{k}" if k[0] == "_"
    @fields[k]
  end

  def []=(k, v)
    raise "undefined internal field #{k}" if k[0] == "_"
    @fields[k] = v
  end

  def method_missing(name, *args, &block)
    if (name.to_s =~ /^(.*)=$/)
      self[$1] = args[0]
    else
      return self[name.to_s]
    end
  end

  def to_s
    k = SchemaSchema.key(schema_class)
    "<BOOT #{schema_class.name} #{k ? self[k.name] : @_id}>"
  end

  def hash
    "boot#{_id}".hash
  end

  def nil?
    false
  end

  def _id
    return @id
  end

  def respond_to?(sym)
    return false
  end

  def inspect
    to_s
  end
end


class ValueHash < Hash
  include Enumerable
  def each(&block)
    values.each &block
  end
  def <<(x)
    self[x.name] = x
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

    def method_missing(name, *args)
      @builder.get_field(@klass, name.to_s)
    end
  end

  @@schemas = {}

  def self.inherited(subclass)
    schema = SchemaModel.new
    @@schemas[subclass.to_s] = schema
    schema.name = subclass.to_s
    schema.classes = ValueHash.new
    schema.primitives = ValueHash.new
    schema.sym_primitives = ValueHash.new
  end

  def self.schema
    @@schemas[self.to_s]
  end

  def self.schema=(schema)
    @@schemas[schema.name] = schema
  end
    

  class << self
    def primitive(name)
      m = SchemaModel.new
      m.name = name.to_s
      schema.primitives[name.to_s] = m
      schema.sym_primitives[name] = m
    end
      
    def klass(wrapped, opts = {}, &block)
      m = wrapped.klass
      m.super = opts[:super] ? opts[:super].klass : nil
      m.super.subtypes << m if m.super
      m.schema = schema
      @@current = m
      yield
    end

    def field(name, opts = {})
      f = get_field(@@current, name.to_s)
      t = opts[:type]
      f.type = schema.sym_primitives.keys.include?(t) ? schema.sym_primitives[t] : t.klass
      f.optional = opts[:optional] || false
      f.many = opts[:many] || false
      f.key = opts[:key] || false
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
      f = SchemaModel.new
      #puts "Creating field #{name} (#{f._id})"
      f.name = name
      klass.fields[name] = f
      f.owner = klass
      return f
    end

    def get_class(name)
      m = schema.classes[name]
      return m if m
      m = SchemaModel.new
      schema.classes[name] = m
      #puts "Getting class #{name} (#{m._id})"
      m.name = name
      m.fields = ValueHash.new
      m.subtypes = ValueHash.new
      return m
    end

    def finalize(schema)
      schema.schema_class = SchemaSchema::Schema.klass
      schema.primitives.each do |p|
        p.schema_class = SchemaSchema::Primitive.klass
      end
      schema.classes.each do |c|
        c.schema_class = SchemaSchema::Klass.klass
        c.fields.each do |f|
          f.schema_class = SchemaSchema::Field.klass
        end
      end
    end
    
    
  end

end
