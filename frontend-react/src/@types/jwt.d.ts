interface JsonWebKeyBase {
    kty: string;
    use?: "sig" | "enc";
    key_ops?: string;
    alg?: string;
    kid?: string;
    x5u?: string;
    x5c?: string;
    x5t?: string;
    "x5t#S256"?: string;
}

interface RSAPublicJsonWebKey extends JsonWebKeyBase {
    kty: "RSA";
    alg?: "RS256" | "RS384" | "RS512";
    n: string;
    e: string;
}

interface RSAPrivateJsonWebKey extends RSAPublicJsonWebKey {
    d: string;
    p?: string;
    q?: string;
    dp?: string;
    dq?: string;
    qi?: string;
    oth?: any[];
}

interface SymmetricJsonWebKey extends JsonWebKeyBase {
    kty: "oct";
    k: string;
}

type RsJsonWebKey =
    | JsonWebKeyBase
    | RSAPublicJsonWebKey
    | RSAPrivateJsonWebKey
    | SymmetricJsonWebKey;

interface RsJsonWebKeySet {
    keys: RsJsonWebKey[];
}
