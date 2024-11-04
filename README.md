# ContentGrid Liaison

Liaison tells ContentGrid frontend apps how to connect to the backend.

## Purpose

The frontend should have a `<script>` tag that refers to `/config.js`. The
gateway will route that to liaison, which will respond with a javascript file
that looks like this:

```js
window.contentGridConfig = {
    v1: {
        baseUrl: "https://12345678-9abc-def0-1234-56789abcdef0.contentgrid.cloud/",
        oidc: {
            authority: "https://auth.contentgrid.com/realms/cg-12345678-9abc-def0-1234-56789abcdef0",
            client_id: "my-webapp",
        }
    }
};
```

With this information, the frontend can authenticate and connect to the backend.

## Workings

Liaison reads from the runtime-platform Kubernetes cluster. It starts an
informer that looks for ConfigMaps with the label `app.contentgrid.com/service-type: webapp`
and remembers an entry per domain described in the ConfigMap. It serves an
endpoint at `/config.js` which inspects the Host header. If this header matches
one of the discovered domains, it responds with a javascript snippet like above
containing information on how to connect to the relevant ContentGrid app.
