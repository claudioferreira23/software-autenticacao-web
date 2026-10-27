# Portal DS — Frontend

This project was generated using [Angular CLI](https://github.com/angular/angular-cli) version 22.2.4.

## Development server

Start the backend on http://localhost:8080, then start the frontend with:

```bash
npm start
```

Once the server is running, open your browser and navigate to `http://localhost:4200/`. The application will automatically reload whenever you modify any of the source files.

Requests to `/api` are forwarded to the local backend by `proxy.conf.json`. The app provides:

- account creation at `/cadastro`;
- login at `/login`;
- session restoration after a page reload;
- protected identity page at `/perfil`;
- server-side logout.

Authentication uses the backend HTTP session. The `JSESSIONID` cookie is `HttpOnly`; Angular keeps the CSRF token returned by the backend only in memory and sends it in the `X-XSRF-TOKEN` header. No authentication data is stored in `localStorage` or `sessionStorage`.

## Code scaffolding

Angular CLI includes powerful code scaffolding tools. To generate a new component, run:

```bash
ng generate component component-name
```

For a complete list of available schematics (such as `components`, `directives`, or `pipes`), run:

```bash
ng generate --help
```

## Building

To build the project run:

```bash
ng build
```

This will compile your project and store the build artifacts in the `dist/` directory. By default, the production build optimizes your application for performance and speed.

## Running unit tests

To execute unit tests with the [Vitest](https://vitest.dev/) test runner, use the following command:

```bash
ng test
```

## Running end-to-end tests

For end-to-end (e2e) testing, run:

```bash
ng e2e
```

Angular CLI does not come with an end-to-end testing framework by default. You can choose one that suits your needs.

## Additional Resources

For more information on using the Angular CLI, including detailed command references, visit the [Angular CLI Overview and Command Reference](https://angular.dev/tools/cli) page.
