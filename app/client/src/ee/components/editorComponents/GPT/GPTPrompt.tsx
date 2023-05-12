import React, { useCallback, useEffect, useMemo } from "react";
import SyntaxHighlighter from "react-syntax-highlighter/dist/cjs/prism-light";
import { marked } from "marked";
import CopyIcon from "remixicon-react/ClipboardLineIcon";
import { duotoneLight } from "react-syntax-highlighter/dist/cjs/styles/prism";
import styled from "styled-components";
import copy from "copy-to-clipboard";
import { useLocation } from "react-router";
import { getEntityInCurrentPath } from "sagas/RecentEntitiesSagas";
import AnalyticsUtil from "utils/AnalyticsUtil";
import type {
  TChatGPTPrompt,
  TErrorPrompt,
  TUserPrompt,
  TAssistantPrompt,
} from "./utils";
import { GPTTask } from "./utils";
import { GPT_TASKS } from "./utils";
import { isGPTErrorPrompt } from "./utils";
import { isUserPrompt, isAssistantPrompt } from "./utils";
import { Icon, Spinner, AppIcon } from "design-system-old";
import { selectEvaluatedResult } from "./utils";
import Play from "remixicon-react/PlayLineIcon";
import { useDispatch, useSelector } from "react-redux";
import { ReduxActionTypes } from "@appsmith/constants/ReduxActionConstants";
import ReadOnlyEditor from "components/editorComponents/ReadOnlyEditor";
import { isEmpty } from "lodash";
import classNames from "classnames";
import LikeIcon from "remixicon-react/ThumbUpLineIcon";
import DislikeIcon from "remixicon-react/ThumbDownLineIcon";
import { Colors } from "constants/Colors";
import sql from "react-syntax-highlighter/dist/cjs/languages/prism/sql";
SyntaxHighlighter.registerLanguage("sql", sql);

const ResponseContainer = styled.div`
  background: #f5f5f5;
  pre:first-of-type {
    overflow: unset !important;
    border-radius: 0.25rem;
    padding: 0 8px 4px !important;
    margin: 0 !important;
    font-size: 0.75em;
    background: transparent !important;
    code {
      font-size: 13px !important;
      word-break: break-word !important;
      white-space: pre-wrap !important;
      background: transparent !important;
      .token {
        background: transparent !important;
      }
    }
  }
`;

const ResultContainer = styled.div`
  .ur--has-border {
    min-height: 0;
  }
  .cm-s-duotone-light {
    border: none !important;
  }
`;

export const UserPromptWrapper = styled.div`
  color: black;
  font-size: 13px;
  font-weight: 400;
  padding: 8px;
  font-style: normal;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
`;

type TGPTPromptProps = {
  prompt: TChatGPTPrompt;
};

export function GPTPrompt(props: TGPTPromptProps) {
  const { prompt } = props;
  if (isUserPrompt(prompt)) {
    return <UserPrompt prompt={prompt} />;
  } else if (isAssistantPrompt(prompt)) {
    return <AssistantPrompt prompt={prompt} />;
  } else if (isGPTErrorPrompt(prompt)) {
    return <ErrorPrompt prompt={prompt} />;
  }
  return null;
}

function UserPrompt(props: { prompt: TUserPrompt }) {
  const { content } = props.prompt;
  return (
    <div className="flex w-full justify-end items-center">
      <UserPromptWrapper className="rounded border border-[#f0f0f0]">
        {content}
      </UserPromptWrapper>
    </div>
  );
}

function ErrorPrompt(props: { prompt: TErrorPrompt }) {
  const { content } = props.prompt;
  return (
    <div className="flex w-full justify-end items-center">
      <UserPromptWrapper className="rounded border border-[#f0f0f0] bg-red-100 gap-2 items-center">
        <Icon className="flex-shrink-0" name="error" size={16} />
        {content}
      </UserPromptWrapper>
    </div>
  );
}

function AssistantPrompt(props: { prompt: TAssistantPrompt }) {
  const { content, liked, messageId, query, task } = props.prompt;
  const [copyIconClicked, clickCopyIcon] = React.useState(false);
  const location = useLocation();
  const pageType = useMemo(() => {
    return getEntityInCurrentPath(location.pathname).pageType;
  }, [location.pathname]);
  const evaluatedExpressionResult = useSelector(
    selectEvaluatedResult(messageId || ""),
  );
  const evaluatedExpressionRef = React.useRef<HTMLDivElement>(null);

  useEffect(() => {
    setRunningSnippet(false);
    setTimeout(() => {
      evaluatedExpressionRef.current?.scrollIntoView(true);
    });
  }, [evaluatedExpressionResult]);

  const [runningSnippet, setRunningSnippet] = React.useState(false);
  const [resultsOpen, setResultsOpen] = React.useState(true);

  const dispatch = useDispatch();

  const handlePlay = useCallback(() => {
    setRunningSnippet(true);
    dispatch({
      type: ReduxActionTypes.EVALUATE_GPT_RESPONSE,
      payload: {
        expression: content,
        task,
        messageId,
      },
    });
    setTimeout(() => setRunningSnippet(false), 5000);
  }, [content]);

  const handleCopy = useCallback(() => {
    let copyText = content;
    if (pageType === "canvas") {
      copyText = `{{${content}}}`;
    }
    copy(copyText);
    clickCopyIcon(true);
    setTimeout(() => {
      clickCopyIcon(false);
    }, 2000);
    AnalyticsUtil.logEvent("AI_RESPONSE_COPIED", {
      responseId: messageId,
      requestedOutputType: task,
      generatedCode: content,
      userQuery: query,
    });
  }, [content, clickCopyIcon, pageType]);

  const logFeedback = useCallback(
    (liked: boolean) => {
      AnalyticsUtil.logEvent("AI_RESPONSE_FEEDBACK", {
        responseId: messageId,
        requestedOutputType: task,
        liked,
        generatedCode: content,
        userQuery: query,
      });
      dispatch({
        type: ReduxActionTypes.UPDATE_GPT_MESSAGE,
        payload: {
          messageId,
          liked,
        },
      });
    },
    [content, messageId, task],
  );

  const parsedDocument = marked(content);
  const domParser = new DOMParser();
  const documentObj = domParser.parseFromString(parsedDocument, "text/html");
  const text = documentObj.body.innerText?.trim() || "";

  const tagName = GPT_TASKS.find((t) => t.id === task)?.title;

  return (
    <div className="flex flex-col">
      <ResponseContainer
        className={classNames({
          "relative rounded gap-2 border border-[#f0f0f0]": true,
          "rounded-b-none": evaluatedExpressionResult || runningSnippet,
        })}
      >
        <div className="pl-2 font-medium text-xs bg-[#e2e2e2] capitalize flex justify-between items-center">
          {tagName}
          <div className="flex items-center justify-end gap-[2px]">
            <div
              className=" hover:bg-[#cfcfcf] p-1 gap-[2px] flex text-[10px] items-center cursor-pointer"
              onClick={handleCopy}
            >
              <CopyIcon size={13} />
              {copyIconClicked ? "Copied!" : "Copy"}
            </div>
            {task !== GPTTask.SQL_QUERY && (
              <div
                className="hover:bg-[#cfcfcf] p-1 gap-[2px] flex text-[10px] items-center cursor-pointer"
                onClick={handlePlay}
              >
                <Play size={15} />
                Run code
              </div>
            )}
          </div>
        </div>
        <div className="flex pt-2 pb-1 flex-col">
          <SyntaxHighlighter
            language={task === GPTTask.SQL_QUERY ? "sql" : "javascript"}
            style={duotoneLight}
          >
            {text}
          </SyntaxHighlighter>
        </div>
        <div className="flex flex-row justify-end items-center px-1 pb-1">
          <div className="flex gap-[2px] transition-all">
            <div
              className={classNames({
                "p-1 hover:bg-gray-200 cursor-pointer": true,
                hidden: liked === false,
                "pointer-events-none": liked === true,
              })}
              onClick={() => logFeedback(true)}
            >
              <LikeIcon color={liked === true ? "green" : "black"} size={13} />
            </div>
            <div
              className={classNames({
                "p-1 hover:bg-gray-200 cursor-pointer": true,
                hidden: liked === true,
                "pointer-events-none": liked === false,
              })}
              onClick={() => logFeedback(false)}
            >
              <DislikeIcon
                className="cursor-pointer"
                color={`${liked === false ? Colors.DANGER_SOLID : "black"}`}
                size={13}
              />
            </div>
          </div>
        </div>
      </ResponseContainer>
      {(evaluatedExpressionResult || runningSnippet) && (
        <div
          className={classNames({
            "flex h-auto w-full transition-all justify-center items-center":
              true,
            "rounded-b-sm border border-[#f0f0f0] border-t-0": true,
          })}
        >
          {runningSnippet ? (
            <div className="p-2">
              <Spinner />
            </div>
          ) : (
            <div className="w-full flex flex-col">
              <div
                className={classNames({
                  "px-1 py-1 text-xs cursor-pointer flex flex-row items-center bg-[#f0f0f045]":
                    true,
                })}
                onClick={() => setResultsOpen(!resultsOpen)}
                ref={evaluatedExpressionRef}
              >
                {resultsOpen ? (
                  <AppIcon name="arrow-down" size={12} />
                ) : (
                  <AppIcon name="arrow-right" size={12} />
                )}
                {resultsOpen ? "Hide result" : "Show result"}
              </div>
              <ResultContainer
                className={classNames({
                  "w-full": true,
                  hidden: !resultsOpen,
                })}
              >
                <ReadOnlyEditor
                  folding={false}
                  height="100%"
                  input={{
                    value: isEmpty(evaluatedExpressionResult)
                      ? "null"
                      : evaluatedExpressionResult,
                  }}
                  showLineNumbers={false}
                />
              </ResultContainer>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
