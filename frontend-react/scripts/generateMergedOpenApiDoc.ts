import { merge, isErrorResult, MergeInput } from 'openapi-merge';
import { Swagger } from 'atlassian-openapi';
import { parse, stringify } from 'yaml'
import { glob } from 'glob'
import { join, dirname, basename } from 'node:path'
import { fileURLToPath } from 'node:url'
import { readFileSync, writeFileSync } from 'node:fs'
import { exit } from 'node:process';

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
}

/**
 * OpenApi doesn't support a clean way to break up paths into 
 * multiple files natively, so we manually merge all the root
 * docs into one before using any openapi tools. The root docs
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

  const files = [rootFile, fhirFile, ...await glob([`${docDir}/*.yaml`], { ignore: [rootFile, outFile, fhirFile] })]
  const docs: Swagger.SwaggerV3[] = files.map(f => {
    let docStr = readFileSync(f, { encoding: 'utf-8' })

    // remove filename from refs that contain fhir file name to convert to local ref
    docStr = docStr.replace(fhirRelativeRefFile, "")

    return parse(docStr)
  });

  // Keep only tags and components from fhir doc
  docs[1] = {
    ...docs[0],
    tags: docs[1].tags,
    components: docs[1].components
  }

  const inputs: MergeInput = docs.map(oas => ({ oas }))
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
  await saveFhirOpenApiDoc(join(docDir, "components/fhir.yaml"));
  await mergeRootDocs(docDir)
} catch (e: any) {
  console.error(e)
  exit(1)
}