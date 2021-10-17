{{/*
Expand the name of the chart.
*/}}
{{- define "codefreak.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "codefreak.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "codefreak.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "codefreak.labels" -}}
helm.sh/chart: {{ include "codefreak.chart" . }}
{{ include "codefreak.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "codefreak.selectorLabels" -}}
app.kubernetes.io/name: {{ include "codefreak.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "codefreak.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "codefreak.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{- define "codefreak.serviceAccountRoleName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "codefreak.fullname" .) .Values.serviceAccount.roleName }}
{{- else }}
{{- default "default" .Values.serviceAccount.roleName }}
{{- end }}
{{- end }}

{{- define "codefreak.applicationYmlContent" -}}
{{- $defaultConfig := tpl (.Files.Get "files/conf/application.yml") . | fromYaml }}
{{- merge .Values.applicationYmlOverrides $defaultConfig | toYaml }}
{{- end }}

{{- define "codefreak.publicUrl" -}}
{{- if .Values.publicUrl -}}
{{- trimSuffix "/" .Values.publicUrl }}
{{- else if .Values.ingress.enabled -}}
{{- $host := first .Values.ingress.hosts -}}
{{- $path := first $host.paths -}}
http{{ if $.Values.ingress.tls }}s{{ end }}://{{ $host.host }}{{ trimSuffix "/" $path.path }}
{{- else -}}
http://{{ include "codefreak.fullname" . }}.{{ .Release.Namespace }}.svc.cluster.local:{{ .Values.service.port }}
{{- end }}
{{- end -}}
