package co.blocke.scalajack

import reflect.runtime.currentMirror
import reflect.runtime.universe._
import scala.collection.concurrent.TrieMap
import scala.reflect.NameTransformer._
import fields._

object Analyzer {

	private val classRepo    = new TrieMap[String,Field]()
	private val typeRepo     = new TrieMap[String,FieldMirror]()

	private val ru           = scala.reflect.runtime.universe
	private val mongoType    = ru.typeOf[MongoKey]
	
	/**
	 * Given a class name, figure out the appropriate Field object
	 */
	def apply[T]( cname:String )(implicit m:Manifest[T]) : Field = {
		val ccf = classRepo.get(cname)
		if( ccf.isDefined && m.typeArguments.size == 0 ) 
			ccf.get
		else {
			val clazz  = Class.forName(cname)
			val symbol = currentMirror.classSymbol(clazz)
			val symbolType = symbol.typeSignature
			if( m.typeArguments.size > 0 ) {
				resolveCCTypes( m.typeArguments.map( _.toString ), cname, ()=>inspect[T]("", symbolType).asInstanceOf[CaseClassField] )
			} else {
				val v = inspect[T]("", symbolType)
				classRepo.put(cname, v)
				v
			}
		}
	}

	private def inspect[T]( fieldName:String, ctype:Type, inContainer:Boolean = false, classCompanionSymbol:Option[Symbol] = None ) : Field = {

		val fullName = ctype.typeSymbol.fullName.toString

		if( fullName.toString == "scala.collection.immutable.List" ) {
			ctype match {
				case TypeRef(pre, sym, args) => ListField(fieldName, inspect( fieldName, args(0), true ))
			}
		} else if( fullName == "scala.Enumeration.Value" ) {
			val erasedEnumClass = Class.forName(ctype.asInstanceOf[TypeRef].toString.replace(".Value","$"))
			val enum = erasedEnumClass.getField(MODULE_INSTANCE_NAME).get(null).asInstanceOf[Enumeration]
			EnumField( fieldName, enum)
		} else if( fullName == "scala.Option" ) {
			val subtype = ctype.asInstanceOf[TypeRef].args(0)
			// Facilitate an Option as a Mongo key part (a very bad idea unless you are 100% sure the value is non-None!!!)
			val subField = inspect(fieldName, subtype, true, classCompanionSymbol)
			OptField( fieldName, subField, subField.hasMongoAnno )
		} else if( fullName == "scala.collection.immutable.Map" ) {
			val valuetype = ctype.asInstanceOf[TypeRef].args(1)
			MapField( fieldName, inspect(fieldName, valuetype, true) )
		} else {
			val sym = currentMirror.classSymbol(Class.forName(fullName))
			if( sym.isTrait && !fullName.startsWith("scala"))
				TraitField( fieldName )
			else if( sym.isCaseClass ) {
				val typeArgs = { 
					if( ctype.takesTypeArgs ) {
						val poly = ctype.asInstanceOf[PolyType].typeParams
						poly.map( p => p.name.toString )
					} else List[String]()
				}
				// Find and save the apply method of the companion object
				val companionClazz = Class.forName(fullName+"$")
				val companionSymbol = currentMirror.classSymbol(companionClazz)
				val caseObj = companionClazz.getField(MODULE_INSTANCE_NAME).get(null)
				val applyMethod = companionClazz.getMethods.find( _.getName == "apply" ).get
				
				// Build the field list
				val constructor = ctype.members.collectFirst {
					case method: MethodSymbol
						if method.isPrimaryConstructor && method.isPublic && !method.paramss.isEmpty && !method.paramss.head.isEmpty => method
				}.getOrElse( throw new IllegalArgumentException("Case class must have at least 1 public constructor having more than 1 parameters."))
				val fields = constructor.paramss.head.map( c => { 
					if( typeArgs.contains( c.typeSignature.toString ) )
						TypeField( c.name.toString, c.typeSignature.toString )
					else 
						inspect(c.name.toString, c.typeSignature, false, Some(companionSymbol)) 
				})

				CaseClassField( fieldName, ctype, fullName, applyMethod, fields, caseObj, typeArgs )
			} else {
				// See if there's a MongoKey annotation on any of the class' fields
				val mongoAnno = classCompanionSymbol.fold(List[String]())( (cs) => {
					cs.typeSignature.members.collectFirst {
						case method:MethodSymbol if( method.name.toString == "apply") => method.paramss.head.collect{ case p if( p.annotations.find(a => a.tpe == Analyzer.mongoType).isDefined) => p.name.toString }
					}.getOrElse(List[String]())
				})
				fullName match {
					case "java.lang.String" => StringField( fieldName, mongoAnno.contains(fieldName) )
					case "scala.Int"        => IntField( fieldName, mongoAnno.contains(fieldName)    )
					case "scala.Char"       => CharField( fieldName, mongoAnno.contains(fieldName)   )
					case "scala.Long"       => LongField( fieldName, mongoAnno.contains(fieldName)   )
					case "scala.Float"      => FloatField( fieldName, mongoAnno.contains(fieldName)  )
					case "scala.Double"     => DoubleField( fieldName, mongoAnno.contains(fieldName) )
					case "scala.Boolean"    => BoolField( fieldName, mongoAnno.contains(fieldName)   )
					case "org.bson.types.ObjectId" => ObjectIdField( fieldName )
					case _                  => {
						if( isValueClass(sym) ) {
							val clazz = Class.forName(fullName)
							// Class name transformation so Analyzer will work
							val className = clazz.getDeclaredFields.head.getType.getName match {
								case "int"     => "scala.Int"
								case "char"    => "scala.Char"
								case "long"    => "scala.Long"
								case "float"   => "scala.Float"
								case "double"  => "scala.Double"
								case "boolean" => "scala.Boolean"
								case t         => t
							}
							if( inContainer )
								ValueClassField( fieldName, mongoAnno.contains(fieldName), Analyzer( className ), clazz.getConstructors()(0), findExtJson(fullName) ) //, clazz.getConstructors.toList.head )
							else 
								ValueClassFieldUnboxed( fieldName, mongoAnno.contains(fieldName), Analyzer( className ), findExtJson(fullName) ) //, clazz.getConstructors.toList.head )
						} else
							throw new IllegalArgumentException("Unknown/unsupported data type: "+fullName)
					}
				} 
			}
		}
	}

	private def resolveCCTypes[T](argNames:List[String], cname:String, finderFn: ()=>CaseClassField)(implicit m:Manifest[T]) = {
		// Figure out typed class name
		// Inspect it if not already in cache
		val tcname = cname + argNames.mkString("[",",","]")
		classRepo.get( tcname ).fold( {
			val targTypes = argNames.map( tp =>
				tp match {
					case "String"  => (n:String) => StringField( n, false )
					case "Int"     => (n:String) => IntField( n, false    )
					case "Long"    => (n:String) => LongField( n, false   )
					case "Double"  => (n:String) => DoubleField( n, false )
					case "Float"   => (n:String) => FloatField( n, false  )
					case "Boolean" => (n:String) => BoolField( n, false   )
					case "Char"    => (n:String) => CharField( n, false   )
					case t         => (n:String) => Analyzer(t)
				}
			)
			val v = finderFn()
			val typeMap = v.typeArgs.zip( targTypes ).toMap
			val resolvedField = v.copy( typeArgs = List[String](), fields = v.fields.map( {
					case tf : TypeField => typeMap(tf.symbol)( tf.name )
					case f => f
				}))
			classRepo.put( tcname, resolvedField )
			resolvedField
		} )(c => c.asInstanceOf[CaseClassField])
	}

	private[scalajack] def registerParamClass[T]( target:T, cfield:CaseClassField )(implicit tag:TypeTag[T]) = { 
		val targs = tag.tpe match { case TypeRef(_, _, args) => args }
		resolveCCTypes( targs.map( _.toString ), target.getClass.getName, ()=>cfield )
	}

	private val classLoaders = List(this.getClass.getClassLoader)
	private val ModuleFieldName = "MODULE$"

	private def findExtJson(cname:String) : Option[ExtJson] = {
		val clazz = Class.forName(cname)
		val path = if (clazz.getName.endsWith("$")) clazz.getName else "%s$".format(clazz.getName)
		val c = resolveClass(path, classLoaders)
		if (c.isDefined) {
			val co = c.get.getField(ModuleFieldName).get(null)
			if( co.isInstanceOf[ExtJson] ) Some(co.asInstanceOf[ExtJson])
			else None
		}
		else None
	}

	// Pulled this off Stackoverflow... Not sure if it's 100% effective, but seems to work!
	private def isValueClass( sym:ClassSymbol ) = sym.asType.companionSymbol.typeSignature.members.exists(_.name.toString.endsWith("$extension"))

	private def resolveClass[X <: AnyRef](c: String, classLoaders: Iterable[ClassLoader]): Option[Class[X]] = classLoaders match {
		case Nil      => sys.error("resolveClass: expected 1+ classloaders but received empty list")
		case List(cl) => Some(Class.forName(c, true, cl).asInstanceOf[Class[X]])
		case many => {
			try {
				var clazz: Class[_] = null
				val iter = many.iterator
				while (clazz == null && iter.hasNext) {
					try {
						clazz = Class.forName(c, true, iter.next())
					} catch {
						case e: ClassNotFoundException => 
					}
				}
				if (clazz != null) Some(clazz.asInstanceOf[Class[X]]) else None
			} catch {
				case _: Throwable => None
			}
		}
	}
}
