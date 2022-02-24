# ReportStream Network Interfacing

### A brief history

Prior to handling our own network interface layer, we utilized `coinbase/rest-hooks`, which took a class-based approach to defining an API interface. This is a great way for handling our own API interfaces, too, as it allows us to maintain some of their great features (like default values and type safety) while giving us the freedom to expand, reduce, or alter the pattern to better suit our needs later on. This need for expansion was the reason we dropped the library and built our own layer.

## The `Api` Class

To start creating an interface for an API, your class should extend the `Api` abstract class. This houses the following static variables and methods:

- `accessToken`: The user's stored Okta token
- `organization`: The user's stored organization
- `baseUrl`: This is to **always** be overridden in child classes with the base URL for the API
- `config`: An AxiosRequestConfig that serves as our global default. This should be overridden **if** the API you're interfacing with needs additional headers, an alternative response type, etc. Details on AxiosRequestConfigs can be found in [their documentation](https://axios-http.com/docs/req_config).
- `generateEndpoint()`: A helper function that generates an `Endpoint` that's later used by the network call. These are what we return when defining an endpoint for our child class

It isn't too often you'll need to reference anything but the endpoint generator method in a child class. Let's create one to see why!

## Creating an API Interface

As mentioned above, the first task is to create a new class that extends our `Api` abstract class and overrides the `baseUrl` variable.

```typescript
class NewApi extends Api {
    baseUrl = "/api/new"

    /* 
        This class already acts as an access point for the
        static members of the API class. Next we're going to
        define the characteristics of our API; both data shapes
        and endpoints
    */
}
```

> NOTE: the `baseUrl` variable should exclude everything leading up to, and including, the end of the application's url. (i.e. excluding http://localhost:7071)

Now we're free to start using the `generateEndpoint()` method to define our API's endpoints. The method takes two params, a URL and a class extending `Api`, and just squashes them into the `Endpoint` interface as declared in the Api module.

```typescript
class NewApi extends Api {
    baseUrl = "/api/new"

    const list = (): Endpoint => {
        return NewApi.generateEndpoint(this.baseUrl, this)
    }

    const detail = (id: string): Endpoint => {
        return NewApi.generateEndpoint(`${this.baseUrl}/${id}`, this)
    }
}
```

Borrowing the naming logic from our prior library, `list()` is our endpoint declaration for receiving the full list of `/api/new` results. Meanwhile, the `detail()` endpoint requires an `id` parameter to extend the URL and seek the detailed response of just one result.

Before we continue on to integrating this in a component, we're going to need to define the shape of the data fetched from our API; what exactly is a `New` and what properties does it hold? We could do this with a TypeScript `interface`, but that wouldn't allow us to define default values. A class, however, would! Additionally, we can nest class-based data shapes for more robust type safety.

```typescript
export class New {
    propertyOne: string = ""
    propertyTwo: int = -1
    olds: Old[] = []
}

class Old {
    oldPropertyOne: ""
}

// ... class NewApi is down here
```

## Implement an API Interface

Now that we have our interface class and data shape classes set up, let's look at how we implement this in a component. React relies on state to hold mutable data that needs to trigger UI updates and effects for async tasks that affect the UI/state. For this reason, our own custom hook, `useNetwork` implements the `useState` and `useEffect` hooks. This reduces the implementation to a single line in a component:

```typescript
const { loading, data, status, message } = useNetwork<New[]>(NewApi.list())
```

#### Destructured Response

First, the destructuring is a handy way of accessing all parts of the response to use across your component. The response type follows the following shape: 

```typescript
export interface ResponseType<T> {
    loading: boolean;
    data?: T;
    status: number;
    message: string;
}
```
> NOTE: Data is of type T or undefined, so ensure you're checking for data presence through conditional renders and optional chaining patterns.

A departure from the previous library also means parting ways with the previous error boundary pattern, as well. Now, inside of a component (rather than at the level above it), we can check `loading` to conditionally display our `<Spinner/>` and pipe `status` and `message` to any error communication in our user interface with ease.

#### Type Casting

```
useNetwork<New[]>(...)
```

Our custom network hook is designed to be generic, meaning we can cast any type in via angle brackets and ensure the response is typed properly. In JavaScript, we wouldn't care, but here, this allows our text editors and IDEs to provide proper auto-complete values, and alert us to type errors, undefined properties, etc.

#### Endpoint Parameter

Finally, the network hook accesses the endpoint parameter that's passed in to make the network call with the proper URL and using the proper AxiosRequestConfig. Because of how this is set up, we are able to reduce the logic within our child class methods to just configuring such an endpoint. Should any further developments be needed, we can expand the `Endpoint` interface and `useNetwork` hook to handle the bulk of the it, rather than having to alter a multitude of network calls in various modules and components.

# Areas to Improve

While this is a great new tool at our disposal, nothing is ever truly perfect. In the future, we should consider the following:

- Needs caching!
- How can we better handle pagination?
- Is there a way to streamline error handling?