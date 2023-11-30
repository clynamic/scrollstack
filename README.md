# Scrollstack

Scrollstack (from "a stack of scrolls") is the backend API server for the clynamic profile pages.

## Usage

Scrollstack is deployed to the [clynamic.net](https://clynamic.net) domain.

To execute requests other than `GET`, you need to be authenticated with a token.

### Authentication

Right now, only the administrator can authenticate.
To authenticate, use an environment variable:

```env
ADMIN_TOKEN=your-token
```

The token may be any string, but it is recommended to use a UUID.

### Endpoints

Scrollstack serves a full swagger documentation at `/swagger-ui`.

You may also test the API via [Hoppscotch](https://hoppscotch.io/) and the following spec file:

[api_collection.json](./api_collection.json)

## Build

Scrollstack is written in Kotlin and uses Gradle as a build tool.

To build the project, run:

```sh
./gradlew build
```

## TODO

- [ ] Add user profile pictures and banners
- [ ] Add user accounts  
  (Username, Email, Password, GitHub OAuth?)
- [ ] Add permission system based on creator
- [ ] Add raw projects
- [ ] Add blob storage (CDN)
- [ ] Add more search query parameters

## Frontend

For the frontend, see [Lorelense](https://github.com/clynamic/lorelense).