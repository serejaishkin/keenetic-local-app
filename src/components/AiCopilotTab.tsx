import React, { useState } from 'react';
import {
  ShieldCheck,
  Zap,
  Sparkles,
  Send,
  Wifi,
  Lock,
  Activity,
  CheckCircle2,
  AlertCircle,
  HelpCircle,
  MessageSquare,
  Bot,
  User
} from 'lucide-react';
import { AiDiagnosticResult, ChatMessage, SystemStatus, WanStatus, WifiInterface, ClientDevice } from '../types';

interface AiCopilotTabProps {
  system: SystemStatus | null;
  wan: WanStatus | null;
  wifiList: WifiInterface[];
  clients: ClientDevice[];
  onRunDiagnostics: () => Promise<AiDiagnosticResult>;
  onAskCopilot: (question: string) => Promise<string>;
}

export const AiCopilotTab: React.FC<AiCopilotTabProps> = ({
  system,
  wan,
  wifiList,
  clients,
  onRunDiagnostics,
  onAskCopilot,
}) => {
  const [diagnosticResult, setDiagnosticResult] = useState<AiDiagnosticResult | null>({
    healthScore: 94,
    statusRating: 'Optimal',
    summary: 'Маршрутизатор Keenetic работает стабильно. Нагрузка на процессор в пределах нормы (14%), достаточный запас оперативной памяти (71% свободно), гигабитный WAN канал без задержек.',
    wifiRecommendations: [
      'Диапазон 5 GHz настроен на чистый канал 36 (80 MHz) с высокой пропускной способностью.',
      'Рекомендуется включить Fast Roaming (802.11r/k/v) для мгновенного перехода смартфонов между узлами Mesh.',
      'Ширина 2.4 GHz ограничена 20 MHz, что снижает уровень интерференции в многоквартирном доме.',
    ],
    securityFindings: [
      'Используется современный комбинированный режим шифрования WPA2/WPA3 Mixed.',
      'Уязвимых портов в глобальной сети (WAN) не обнаружено.',
      'Заблокировано 1 подозрительное устройство в черном списке хотспота.',
    ],
    performanceInsights: [
      'Потоковое 4K видео на телевизоре LG OLED потребляет до 25 Мбит/с без задержек.',
      'Проводной накопитель Synology NAS утилизирует гигабитный дуплекс без дропов.',
    ],
    quickActions: [
      {
        title: 'Включить Cloudflare DoH (DNS-over-HTTPS)',
        description: 'Шифрует все DNS запросы на 1.1.1.1 и снижает время отклика доменов',
        impact: 'High',
      },
      {
        title: 'Настроить расписание гостевой сети',
        description: 'Автоматически отключает гостевой Wi-Fi в ночные часы',
        impact: 'Medium',
      },
    ],
  });

  const [isRunningAudit, setIsRunningAudit] = useState(false);

  // Chat State
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([
    {
      id: '1',
      sender: 'assistant',
      text: 'Здравствуйте! Я Keenetic AI Copilot — ваш интеллектуальный помощник по настройке и оптимизации роутера Keenetic. Могу проанализировать каналы Wi-Fi, подсказать RCI/CLI команды или помочь с настройкой приоритетов скорости (QoS) и VPN.',
      timestamp: 'Сейчас',
    },
  ]);
  const [inputQuestion, setInputQuestion] = useState('');
  const [isAsking, setIsAsking] = useState(false);

  const presetQuestions = [
    'Как оптимизировать скорость Wi-Fi 5 GHz?',
    'Кто больше всего нагружает интернет-канал?',
    'Как закрепить статический IP за ноутбуком?',
    'Как настроить гостевую изолированную сеть?',
  ];

  const handleRunAudit = async () => {
    setIsRunningAudit(true);
    try {
      const result = await onRunDiagnostics();
      setDiagnosticResult(result);
    } catch (err) {
      console.error('Audit failed', err);
    } finally {
      setIsRunningAudit(false);
    }
  };

  const handleSendMessage = async (textToSend?: string) => {
    const query = textToSend || inputQuestion;
    if (!query.trim() || isAsking) return;

    const userMsg: ChatMessage = {
      id: Math.random().toString(),
      sender: 'user',
      text: query,
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setChatMessages(prev => [...prev, userMsg]);
    setInputQuestion('');
    setIsAsking(true);

    try {
      const replyText = await onAskCopilot(query);
      const assistantMsg: ChatMessage = {
        id: Math.random().toString(),
        sender: 'assistant',
        text: replyText,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setChatMessages(prev => [...prev, assistantMsg]);
    } catch (err) {
      const errorMsg: ChatMessage = {
        id: Math.random().toString(),
        sender: 'assistant',
        text: 'Извините, произошла ошибка при формировании ответа. Попробуйте еще раз.',
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setChatMessages(prev => [...prev, errorMsg]);
    } finally {
      setIsAsking(false);
    }
  };

  return (
    <div className="space-y-6">
      {/* Top Banner with Audit Trigger */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center space-x-2">
            <Sparkles className="w-5 h-5 text-purple-400" />
            <h2 className="text-base font-semibold text-[#fafafa]">Keenetic AI Copilot & Диагностический Центр</h2>
          </div>
          <p className="text-xs text-zinc-400 mt-1">
            Интеллектуальный анализ конфигурации KeeneticOS, безопасности Wi-Fi и распределения трафика
          </p>
        </div>

        <button
          onClick={handleRunAudit}
          disabled={isRunningAudit}
          className="px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-500 text-white text-xs font-semibold transition flex items-center space-x-2 cursor-pointer shadow-sm self-start sm:self-auto"
        >
          <Sparkles className={`w-4 h-4 ${isRunningAudit ? 'animate-spin' : ''}`} />
          <span>{isRunningAudit ? 'Сканирование...' : 'Запустить AI Аудит'}</span>
        </button>
      </div>

      {/* Health Score & Audit Insights Grid */}
      {diagnosticResult && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Health Score Card (1 col) */}
          <div className="p-6 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm flex flex-col items-center justify-center text-center space-y-4">
            <div className="relative flex items-center justify-center">
              <div className="w-32 h-32 rounded-full border-8 border-[#27272a] flex items-center justify-center">
                <div className="w-28 h-28 rounded-full border-4 border-emerald-400 flex flex-col items-center justify-center bg-[#09090b]">
                  <span className="text-3xl font-black text-white font-mono">{diagnosticResult.healthScore}</span>
                  <span className="text-[10px] uppercase font-bold text-emerald-400">из 100</span>
                </div>
              </div>
            </div>

            <div>
              <span className="px-3 py-1 rounded-full text-xs font-semibold bg-emerald-500/15 text-emerald-300 border border-emerald-500/30 inline-flex items-center gap-1.5">
                <CheckCircle2 className="w-3.5 h-3.5" />
                Состояние: {diagnosticResult.statusRating}
              </span>
              <p className="text-xs text-zinc-300 mt-3 leading-relaxed text-left">
                {diagnosticResult.summary}
              </p>
            </div>
          </div>

          {/* Key Findings & Recommendations (2 cols) */}
          <div className="lg:col-span-2 p-6 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm space-y-4">
            <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-blue-400" />
              Рекомендации AI по безопасности и Wi-Fi
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              {/* Wi-Fi Column */}
              <div className="p-3.5 rounded-xl bg-[#09090b] border border-[#27272a] space-y-2">
                <span className="font-semibold text-blue-300 flex items-center gap-1.5">
                  <Wifi className="w-3.5 h-3.5" />
                  Wi-Fi Оптимизация
                </span>
                <ul className="space-y-1.5 text-zinc-300">
                  {diagnosticResult.wifiRecommendations.map((rec, i) => (
                    <li key={i} className="flex items-start gap-1.5">
                      <span className="text-blue-400 font-bold">•</span>
                      <span>{rec}</span>
                    </li>
                  ))}
                </ul>
              </div>

              {/* Security Column */}
              <div className="p-3.5 rounded-xl bg-[#09090b] border border-[#27272a] space-y-2">
                <span className="font-semibold text-emerald-300 flex items-center gap-1.5">
                  <Lock className="w-3.5 h-3.5" />
                  Безопасность & Сеть
                </span>
                <ul className="space-y-1.5 text-zinc-300">
                  {diagnosticResult.securityFindings.map((sec, i) => (
                    <li key={i} className="flex items-start gap-1.5">
                      <span className="text-emerald-400 font-bold">•</span>
                      <span>{sec}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            {/* Quick Action Proposals */}
            <div className="space-y-2 pt-2 border-t border-[#27272a]">
              <span className="text-xs font-semibold text-zinc-400 uppercase tracking-wider block">
                Рекомендуемые быстрые действия:
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {diagnosticResult.quickActions.map((action, i) => (
                  <div
                    key={i}
                    className="p-3 rounded-lg bg-[#09090b] border border-[#27272a] flex items-start justify-between gap-2"
                  >
                    <div>
                      <span className="font-semibold text-xs text-[#fafafa] block">{action.title}</span>
                      <span className="text-[11px] text-zinc-400 leading-tight">{action.description}</span>
                    </div>
                    <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-purple-500/15 text-purple-300 border border-purple-500/25 whitespace-nowrap">
                      {action.impact} Impact
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Interactive AI Chat with Keenetic Copilot */}
      <div className="p-5 rounded-xl bg-[#18181b] border border-[#27272a] shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Bot className="w-5 h-5 text-purple-400" />
            <h3 className="text-sm font-semibold text-[#fafafa] uppercase tracking-wider">
              Чат с Keenetic AI Ассистентом
            </h3>
          </div>
          <span className="text-xs text-purple-400 font-mono">Gemini 3.7 Flash Engine</span>
        </div>

        {/* Preset Prompt Suggestions */}
        <div className="flex flex-wrap gap-2">
          {presetQuestions.map((q, idx) => (
            <button
              key={idx}
              onClick={() => handleSendMessage(q)}
              disabled={isAsking}
              className="px-3 py-1.5 rounded-md bg-[#09090b] hover:bg-[#27272a] text-zinc-300 hover:text-white border border-[#27272a] text-xs font-medium transition cursor-pointer"
            >
              {q}
            </button>
          ))}
        </div>

        {/* Message Log */}
        <div className="p-4 rounded-xl bg-[#09090b] border border-[#27272a] min-h-[220px] max-h-[360px] overflow-y-auto space-y-3.5">
          {chatMessages.map(msg => (
            <div
              key={msg.id}
              className={`flex items-start space-x-2.5 ${
                msg.sender === 'user' ? 'justify-end' : 'justify-start'
              }`}
            >
              {msg.sender === 'assistant' && (
                <div className="w-7 h-7 rounded-lg bg-purple-500/15 border border-purple-500/25 flex items-center justify-center text-purple-400 shrink-0 mt-0.5">
                  <Bot className="w-4 h-4" />
                </div>
              )}

              <div
                className={`max-w-[80%] p-3 rounded-xl text-xs leading-relaxed ${
                  msg.sender === 'user'
                    ? 'bg-blue-600 text-white rounded-tr-none'
                    : 'bg-[#18181b] border border-[#27272a] text-zinc-200 rounded-tl-none'
                }`}
              >
                <p className="whitespace-pre-wrap">{msg.text}</p>
                <span
                  className={`block text-[9px] mt-1 ${
                    msg.sender === 'user' ? 'text-blue-200' : 'text-zinc-500'
                  }`}
                >
                  {msg.timestamp}
                </span>
              </div>

              {msg.sender === 'user' && (
                <div className="w-7 h-7 rounded-lg bg-blue-500/15 border border-blue-500/25 flex items-center justify-center text-blue-400 shrink-0 mt-0.5">
                  <User className="w-4 h-4" />
                </div>
              )}
            </div>
          ))}

          {isAsking && (
            <div className="flex items-center space-x-2 text-zinc-400 text-xs italic">
              <Sparkles className="w-3.5 h-3.5 text-purple-400 animate-spin" />
              <span>Keenetic Copilot формулирует ответ...</span>
            </div>
          )}
        </div>

        {/* Input Bar */}
        <div className="flex items-center space-x-2">
          <input
            type="text"
            value={inputQuestion}
            onChange={e => setInputQuestion(e.target.value)}
            onKeyDown={e => {
              if (e.key === 'Enter') handleSendMessage();
            }}
            placeholder="Задайте вопрос о роутере, портах, скорости, VPN или CLI командах..."
            className="flex-1 px-4 py-2.5 rounded-lg bg-[#09090b] border border-[#27272a] text-xs text-white placeholder-zinc-500 focus:outline-none focus:border-purple-500"
          />
          <button
            onClick={() => handleSendMessage()}
            disabled={isAsking || !inputQuestion.trim()}
            className="px-5 py-2.5 rounded-lg bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white text-xs font-semibold transition flex items-center space-x-1.5 cursor-pointer shadow"
          >
            <Send className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Отправить</span>
          </button>
        </div>
      </div>
    </div>
  );
};
