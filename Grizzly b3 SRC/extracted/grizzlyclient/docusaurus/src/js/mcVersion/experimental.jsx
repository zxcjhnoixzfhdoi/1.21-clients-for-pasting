import { usePluginData } from '@docusaurus/useGlobalData'

export default function ExperimentalVersion() {
  const { experimental } = usePluginData('mc-version')
  return <span>{experimental}</span>
}
