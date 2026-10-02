// Dev tool (Phase 0 spike, kept for diagnostics): drives the Offline Protocol SDK directly
// and measures BLE discovery and delivery between two phones.
import * as React from 'react';
import { useEffect, useRef, useState } from 'react';
import { PermissionsAndroid, Platform, ScrollView } from 'react-native';
import { Button, Divider, List, Text, useTheme } from 'react-native-paper';
import { MessagePriority, OfflineProtocol } from '@offline-protocol/mesh-sdk';
import { View } from '../components/Themed';
import { RootStackScreenProps } from '../types';

// D20 config, BLE only (D1). ponytail: hard-coded spike profile; Phase 2 uses atlas-<companyId>-<userId>.
const CONFIG = {
  appId: 'atlas-cmms',
  profile: 'atlas-spike',
  transports: { ble: { enabled: true } },
  encryption: {
    enabled: true,
    requireEncryption: true,
    autoKeyExchange: true,
    storePending: true
  }
};

type Neighbor = { peerId: string; rssi?: number; seenAt: number };

const percentile = (sorted: number[], p: number) =>
  sorted.length
    ? sorted[Math.min(sorted.length - 1, Math.ceil(p * sorted.length) - 1)]
    : null;

const short = (id?: string) => (id ? `${id.slice(0, 10)}…${id.slice(-4)}` : '-');

export default function MeshDiagnosticsScreen({}: RootStackScreenProps<'MeshDiagnostics'>) {
  const theme = useTheme();
  const protoRef = useRef<OfflineProtocol | null>(null);
  const startedAtRef = useRef<number>(0);
  const sentAtRef = useRef<Map<string, number>>(new Map());
  const [running, setRunning] = useState(false);
  const [address, setAddress] = useState<string | null>(null);
  const [transports, setTransports] = useState<string>('-');
  const [neighbors, setNeighbors] = useState<Record<string, Neighbor>>({});
  const [selected, setSelected] = useState<string | null>(null);
  const [firstDiscoveryMs, setFirstDiscoveryMs] = useState<number | null>(null);
  const [latencies, setLatencies] = useState<number[]>([]);
  const [counts, setCounts] = useState({ sent: 0, delivered: 0, received: 0 });
  const [log, setLog] = useState<string[]>([]);

  const append = (line: string) => {
    console.log(`[mesh ${Platform.OS}] ${line}`); // shows in Metro, for collecting measurements
    setLog((prev) =>
      [`${new Date().toISOString().slice(11, 23)} ${line}`, ...prev].slice(0, 200)
    );
  };

  const handleEvent = (e: any) => {
    switch (e.type) {
      case 'identity_ready':
        setAddress(e.address);
        break;
      case 'transport_switched':
        setTransports(e.to);
        break;
      case 'neighbor_discovered':
        setFirstDiscoveryMs((v) => v ?? Date.now() - startedAtRef.current);
        setNeighbors((prev) => ({
          ...prev,
          [e.peer_id]: { peerId: e.peer_id, rssi: e.rssi, seenAt: Date.now() }
        }));
        append(`neighbor_discovered ${short(e.peer_id)} rssi=${e.rssi ?? '?'}`);
        break;
      case 'neighbor_lost':
        setNeighbors((prev) => {
          const { [e.peer_id]: _, ...rest } = prev;
          return rest;
        });
        append(`neighbor_lost ${short(e.peer_id)}`);
        break;
      case 'message_received':
        setCounts((c) => ({ ...c, received: c.received + 1 }));
        append(
          `message_received from ${short(e.sender)} encrypted=${e.encrypted} "${String(e.content).slice(0, 60)}"`
        );
        break;
      case 'message_delivered': {
        const sentAt = sentAtRef.current.get(e.message_id);
        const ms = sentAt ? Date.now() - sentAt : e.latency_ms;
        if (sentAt) {
          sentAtRef.current.delete(e.message_id);
          setLatencies((l) => [...l, ms]);
          setCounts((c) => ({ ...c, delivered: c.delivered + 1 }));
        }
        append(
          `message_delivered ${short(e.message_id)} ${ms}ms${sentAt ? '' : ' (from previous run)'}`
        );
        break;
      }
      case 'message_retrying':
        append(`message_retrying ${short(e.message_id)} retry=${e.retry_count}`);
        break;
      case 'message_failed':
        append(`message_failed ${short(e.message_id)} ${e.reason}`);
        break;
      case 'message_undeliverable':
        append(`message_undeliverable ${short(e.message_id)} ${e.reason}`);
        break;
      case 'diagnostic':
        // SDK transport internals: warnings/errors on screen, info to Metro only, debug dropped (floods)
        if (e.level === 'warning' || e.level === 'error') {
          append(`diag ${e.level}: ${e.message} ${JSON.stringify(e.context ?? {})}`);
        } else if (e.level === 'info') {
          console.log(`[mesh ${Platform.OS}] diag info: ${e.message} ${JSON.stringify(e.context ?? {})}`);
        }
        break;
    }
  };

  const requestPermissions = async () => {
    // iOS prompts for Bluetooth on start().
    if (Platform.OS !== 'android') return append('permissions: iOS prompts on start');
    const p = PermissionsAndroid.PERMISSIONS;
    const wanted =
      (Platform.Version as number) >= 31
        ? [p.BLUETOOTH_SCAN, p.BLUETOOTH_CONNECT, p.BLUETOOTH_ADVERTISE]
        : [p.ACCESS_FINE_LOCATION];
    const res = await PermissionsAndroid.requestMultiple(wanted);
    append(`permissions: ${JSON.stringify(res)}`);
  };

  const start = async () => {
    try {
      const proto = new OfflineProtocol(CONFIG);
      protoRef.current = proto;
      proto.on('all', handleEvent);
      startedAtRef.current = Date.now();
      setFirstDiscoveryMs(null);
      await proto.start();
      setRunning(true);
      setAddress(await proto.localAddress());
      setTransports((await proto.getActiveTransports()).join(', ') || '-');
      append('started');
    } catch (err) {
      append(`start failed: ${err}`);
    }
  };

  const stop = async () => {
    const proto = protoRef.current;
    protoRef.current = null;
    setRunning(false);
    setNeighbors({});
    if (!proto) return;
    proto.removeAllListeners();
    await proto.stop().catch((err) => append(`stop failed: ${err}`));
    append('stopped');
  };

  useEffect(
    () => () => {
      protoRef.current?.removeAllListeners();
      protoRef.current?.stop().catch(() => {});
    },
    []
  );

  const sendTest = async (seq?: number) => {
    if (!protoRef.current || !selected) return;
    try {
      const id = await protoRef.current.sendMessage({
        recipient: selected,
        content: `test ${seq ?? ''} ${Platform.OS} ${new Date().toISOString()}`,
        priority: MessagePriority.High
      });
      sentAtRef.current.set(id, Date.now());
      setCounts((c) => ({ ...c, sent: c.sent + 1 }));
      append(`sent ${short(id)} → ${short(selected)}`);
    } catch (err) {
      append(`send failed: ${err}`);
    }
  };

  const sendBurst = async () => {
    for (let i = 1; i <= 20; i++) {
      await sendTest(i);
      await new Promise((r) => setTimeout(r, 1000));
    }
  };

  const resetStats = () => {
    sentAtRef.current.clear();
    setLatencies([]);
    setCounts({ sent: 0, delivered: 0, received: 0 });
    setLog([]);
  };

  const sorted = [...latencies].sort((a, b) => a - b);

  return (
    <ScrollView style={{ flex: 1, backgroundColor: theme.colors.background }}>
      <View style={{ padding: 16, gap: 8 }}>
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
          <Button mode="outlined" onPress={requestPermissions}>
            Permissions
          </Button>
          <Button mode="contained" disabled={running} onPress={start}>
            Start
          </Button>
          <Button mode="outlined" disabled={!running} onPress={stop}>
            Stop
          </Button>
        </View>
        <Text selectable>Address: {address ?? '-'}</Text>
        <Text>
          Running: {String(running)} · Transport: {transports}
        </Text>
        <Text>
          First discovery: {firstDiscoveryMs != null ? `${(firstDiscoveryMs / 1000).toFixed(1)} s` : '-'}
        </Text>
        <Text>
          Sent {counts.sent} · Delivered {counts.delivered} · Received {counts.received} · Latency
          median {percentile(sorted, 0.5) ?? '-'} ms / p95 {percentile(sorted, 0.95) ?? '-'} ms
        </Text>
        <Divider />
        <Text variant="titleSmall">Neighbours</Text>
        {Object.values(neighbors).map((n) => (
          <List.Item
            key={n.peerId}
            title={short(n.peerId)}
            description={`rssi ${n.rssi ?? '?'} · seen ${new Date(n.seenAt).toLocaleTimeString()}`}
            onPress={() => setSelected(n.peerId)}
            left={(props) => (
              <List.Icon
                {...props}
                icon={selected === n.peerId ? 'radiobox-marked' : 'radiobox-blank'}
              />
            )}
          />
        ))}
        <View style={{ flexDirection: 'row', flexWrap: 'wrap', gap: 8 }}>
          <Button mode="contained" disabled={!running || !selected} onPress={() => sendTest()}>
            Send test
          </Button>
          <Button mode="outlined" disabled={!running || !selected} onPress={sendBurst}>
            Send 20
          </Button>
          <Button onPress={resetStats}>Reset stats</Button>
        </View>
        <Divider />
        <Text variant="titleSmall">Log</Text>
        {log.map((line, i) => (
          <Text key={i} selectable style={{ fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace', fontSize: 11 }}>
            {line}
          </Text>
        ))}
      </View>
    </ScrollView>
  );
}
