import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { createClient } from '@hey-api/openapi-ts';
import { exit } from 'node:process';

const __dirname = dirname(fileURLToPath(import.meta.url))
const docFile = join(__dirname, '../../prime-router/docs/api/openapi.yaml')

try {
  await createClient({
    input: docFile,
    output: 'src/utils/Api',
    client: 'axios',
    format: 'prettier',
    lint: 'eslint'
  });
} catch (e: any) {
  console.error(e)
  exit(1)
}