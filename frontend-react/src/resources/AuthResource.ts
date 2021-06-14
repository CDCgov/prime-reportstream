import { Resource } from '@rest-hooks/rest';


export default class AuthResource extends Resource {
    pk(parent?: any, key?: string): string | undefined {
        throw new Error('Method not implemented.');
    }
    static useFetchInit = (init: RequestInit) => {
      return {
      ...init,
        headers: {
          ...init.headers,
          'Authorization': 'Bearer eyJraWQiOiJxdjdRZWFDRXZxaVlQa05TNEFpQTZWVUZhaFg0MEI0RWo1Y0dvYzk1a2c4IiwiYWxnIjoiUlMyNTYifQ.eyJ2ZXIiOjEsImp0aSI6IkFULkdBbGp3X0pCU2pjdlVtLXZTUnNjNEozdkpWQ05KVjVwZlZTVFI4OVJYVTgiLCJpc3MiOiJodHRwczovL2hocy1wcmltZS5va3RhLmNvbS9vYXV0aDIvZGVmYXVsdCIsImF1ZCI6ImFwaTovL2RlZmF1bHQiLCJpYXQiOjE2MTY0MjgwNTAsImV4cCI6MTYxNjQzMTY1MCwiY2lkIjoiMG9hNmZtOGo0RzF4ZnJ0aGQ0aDYiLCJ1aWQiOiIwMHU1Y3liYzBaMHpwVDg1TjRoNiIsInNjcCI6WyJvcGVuaWQiLCJwcm9maWxlIiwic2ltcGxlX3JlcG9ydCIsImVtYWlsIl0sInN1YiI6InF0djFAY2RjLmdvdiIsIm9yZ2FuaXphdGlvbiI6WyJESHBpbWFfYXpfcGhkIl0sImdpdmVuX25hbWUiOiJNYXR0IiwiZmFtaWx5X25hbWUiOiJZb3VuZyJ9.BLezgNY1M2AK1c4H6y1YcSP2TDN4fZxyX9und4x5W85pMLr1EZc2kT5D57v1eFXmoIq-CuQyMpeQ9hJZWR1E-TJJNAQzuwOF0Mzuo317QdsLjwFrXHitinrDvgazaOC8z7XJ0XRgF8rL15wUzVye4Py2lfV6e5EH2PymgOYyErGLQP16M_KS8V5pSosFYpwSFek5qJNfnK7qDnlMGIbfItLHFqU1sGINt66Xu6j0sh3vuTf5MwNnMCu7B_Q3Xzei2t0zSo13j2Gam-xyNfvEOBp0DUKZBVfoMXZXc2RFPBtKwd5pc5VP5wnmzWffbATgBMRMYS8kqnuE_hWXqTb-qQ',
        },
      }
    };
  }