import { merge, isErrorResult, MergeInput } from 'openapi-merge';
import { Swagger } from 'atlassian-openapi';
import { parse, stringify } from 'yaml'
import { glob } from 'glob'
import { join, dirname, basename } from 'node:path'
import { fileURLToPath } from 'node:url'
import { readFileSync, writeFileSync } from 'node:fs'
import { exit } from 'node:process'
import {handleLint} from '@redocly/cli/lib/commands/lint'
import {commandWrapper} from "@redocly/cli/lib/wrapper"
import { spawnSync } from 'node:child_process';

const FHIR_URL = "https://raw.githubusercontent.com/LinuxForHealth/FHIR/main/fhir-openapi/src/main/webapp/META-INF/openapi.json"
const __dirname = dirname(fileURLToPath(import.meta.url))
const docDir = join(__dirname, '../../prime-router/docs/api')

/**
 * Save FHIR OpenAPI doc to repo
 */
async function saveFhirOpenApiDoc(outFile: string, url = FHIR_URL) {
  const req = await fetch(url)
  if (req.status !== 200) throw new Error("Request failed: " + req.status)

  let docStr = stringify(parse(await req.text()));

  // Fix escaped characters in patterns and descriptions
  docStr = docStr.replace(/(?<!\\)\\([^\\'"\s])/gm, "\\\\$1")

  writeFileSync(outFile, docStr)

  return outFile
}

function prefixFhirDocComponents(file: string): Swagger.SwaggerV3 {
    const origDoc = parse(readFileSync(file, {encoding: 'utf-8'}))

    // mutate origDoc tags and components with prefixes in-place before stringifying
    origDoc.components = Object.fromEntries(Object.entries(origDoc.components).map(([k,v]) => ([k, Object.fromEntries(Object.entries(v as object).map(([k,v]) => [`Fhir${k}`, v]))])))
    origDoc.tags = origDoc.tags.map(t => ({name: `Fhir${t.name}`}))

    let prefixedDocStr = stringify({tags: origDoc.tags, components: origDoc.components})

    // use raw doc string to prefix all references
    prefixedDocStr = prefixedDocStr.replace(/(#\/components\/\w+\/)(\w+)/gm, "$1Fhir$2")

    // return JS doc of final result
    return parse(prefixedDocStr)
}

/**
 * OpenApi doesn't support a clean way to break up paths into 
 * multiple files natively, so we manually merge all the root
 * docs into one before bundling. The root docs
 * do NOT have to follow OpenAPI spec in regards to $refs, as
 * they can assume to locally reference schemas from other root
 * docs that'll be available in the final merged document.
 * 
 * NOTE: references to components/fhir.yaml are automatically
 * converted to local references and the fhir doc components
 * merged due to codegen issues.
 */
async function mergeRootDocs(docDir: string, {rootFilename = "root.yaml", mergeFilename = "openapi.yaml", fhirRelativeRefFile = "components/fhir.yaml"}: {rootFilename?: string, mergeFilename?: string, fhirRelativeRefFile?: string} | undefined = {}) {
  const rootFile = join(docDir, rootFilename)
  const outFile = join(docDir, mergeFilename)
  const fhirFile = join(docDir, fhirRelativeRefFile)
  const fhirRefRegexp = new RegExp(`${fhirRelativeRefFile.replace(".", "\\.")}#/components/schemas/(\\w+)`, "gm")

  const files = [rootFile, ...await glob([`${docDir}/*.yaml`], { ignore: [rootFile, outFile, fhirFile] })]
  const docs: Swagger.SwaggerV3[] = files.map(f => {
    let docStr = readFileSync(f, { encoding: 'utf-8' })

    // remove filename from refs that contain fhir file name to convert to local Fhir-prefixed ref
    docStr = docStr.replace(fhirRefRegexp, "#/components/schemas/Fhir$1")

    return parse(docStr)
  });

  // Add Fhir-prefix to all components to prevent collisions
  const prefixedFhirDoc = prefixFhirDocComponents(fhirFile)

  // Keep only tags and components from fhir doc
  let fhirDoc = {
    ...docs[0],
    tags: prefixedFhirDoc.tags,
    components: prefixedFhirDoc.components
  }

  //console.log(fhirDoc.tags, fhirDoc.components)

  const inputs: MergeInput = [fhirDoc, ...docs].map(oas => ({ oas }))
  const mergeResult = merge(inputs);

  console.log(`📄 Found ${files.length} OpenAPI documents in root of ${docDir}:`)
  for(const f of files) {
    const base = basename(f)
    console.log(`📄   ${base}`)
  }

  if (isErrorResult(mergeResult)) {
    throw new Error(`${mergeResult.message} (${mergeResult.type})`)
  }

  // merge util sets older version
  mergeResult.output.openapi = "3.1.0"

  writeFileSync(outFile, stringify(mergeResult.output));
  console.log(`📄 Merged to ${outFile}`);

  return outFile
}

try {
  const fhirFile = await saveFhirOpenApiDoc(join(docDir, "components/fhir.yaml"));
  const mergedFile = await mergeRootDocs(docDir)

  // skip currently known warnings to reduce noise
  const fhirSkipRules = ["security-defined", "operation-4xx-response", "no-unused-components", "tag-description", "info-license"]
  const fhirSkipRuleOptions = fhirSkipRules.map(r => `--skip-rule=${r}`)
  const fhirLintArgs = ["lint", fhirFile, "--lint-config=error", "--format=summary", ...fhirSkipRuleOptions]

  // skip warnings we don't care about or cannot remedy (ex: bad paths)
  const mergedSkipRules = ["tag-description", "no-unused-components", "no-ambiguous-paths"]
  const mergedSkipRuleOptions = mergedSkipRules.map(r => `--skip-rule=${r}`)
  const mergedLintArgs = ["lint", mergedFile, "--max-problems=1000", ...mergedSkipRuleOptions]
  
  spawnSync("redocly", fhirLintArgs, {stdio: "inherit"})
  spawnSync("redocly", mergedLintArgs, {stdio: "inherit"})

  spawnSync("redocly", ["bundle", mergedFile, "-o", mergedFile], {stdio: "inherit"})
  spawnSync("redocly", mergedLintArgs, {stdio: "inherit"})
} catch (e: any) {
  console.error(e)
  exit(1)
}