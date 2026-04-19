# Headlamp

Headlamp is the current Kubernetes SIG UI project and a practical replacement for the deprecated Kubernetes Dashboard.

By default this setup does not publish the UI to the Internet. Access is intended through `port-forward`.

## Apply

If you use `kubectl` directly on the server:

```powershell
kubectl apply -f k8s/headlamp-namespace.yaml
kubectl apply -f k8s/headlamp-deployment.yaml
kubectl apply -f k8s/headlamp-service.yaml
kubectl apply -f k8s/headlamp-admin-rbac.yaml
kubectl -n headlamp rollout status deployment/headlamp
```

## Access From The Server

If you are already on the server shell:

```powershell
kubectl -n headlamp port-forward svc/headlamp 8100:80
```

Then open:

```text
http://localhost:8100
```

## Access Over SSH

If the cluster is only available on the remote server, the simplest option is:

```powershell
ssh -L 8100:127.0.0.1:8100 root@92.38.48.50 "kubectl -n headlamp port-forward svc/headlamp 8080:80"
```

Then open locally:

```text
http://localhost:8100
```

Keep that terminal open while you use the UI.

## Login Token

Generate an admin token when needed:

```powershell
kubectl -n headlamp create token headlamp-admin
```

If you need the token from your local machine through SSH:

```powershell
ssh root@92.38.48.50 "kubectl -n headlamp create token headlamp-admin"
```

This service account is cluster-admin level and should only be used for admin access.
