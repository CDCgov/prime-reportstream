/** @type {import('ts-jest/dist/types').InitialOptionsTsJest} */
module.exports = {
   modulePaths: [
      "<rootDir>/src"
   ],
   preset: 'ts-jest',
   roots: ["<rootDir>/src"],
   /* INFO
      Test spec file resolution pattern
      Matches parent folder `/src/test` and filename
      should contain `test` (i.e. ComponentTest.d.tsx) 
   */
   testEnvironment: "jsdom",
   testRegex: "(__tests__/.*|(\\.|/)(test|spec))\\.tsx?$",
   /* INFO
      Jest transformations -- this adds support for TypeScript
      using ts-jest 
   */
   transform: {
      "^.+\\.tsx?$": "ts-jest"
   },
   transformIgnorePatterns: [
      "/node_modules/"
   ],

   // Module file extensions for importing
   moduleFileExtensions: ["ts", "tsx", "js", "jsx", "json", "node"]
};