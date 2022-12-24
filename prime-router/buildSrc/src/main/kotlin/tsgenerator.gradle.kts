import me.ntrrgc.tsGenerator.VoidType

val typescriptGenerator = extensions.create<TypeScriptGeneratorExtension>("typescriptGenerator")
typescriptGenerator.voidType.convention(VoidType.UNDEFINED)
typescriptGenerator.intTypeName.convention("number")
typescriptGenerator.typeMappings.convention(mapOf("java.lang.Void" to "undefined"))

tasks.register<TypeScriptGeneratorTask>("generateTypescriptDefinitions") {
    description = "Generates Typescript definitions from Kotlin classes."
    annotation.set(typescriptGenerator.annotation)
    manualClasses.set(typescriptGenerator.manualClasses)
    outputPath.set(typescriptGenerator.outputPath)
    classPath.set(typescriptGenerator.classPath)
    typeMappings.set(typescriptGenerator.typeMappings)
    imports.set(typescriptGenerator.imports)
    intTypeName.set(typescriptGenerator.intTypeName)
    voidType.set(typescriptGenerator.voidType)
}
